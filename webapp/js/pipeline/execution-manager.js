export default class ExecutionManager {
    constructor(pipelineDesigner) {
        this.designer = pipelineDesigner;
        this.sseConnection = null;
        this._animFrameId = null;
        this._animating = false;
        this._pendingFollow = null;
    }

    async _startPipelineExecution(inputs) {
        if (!this.designer.pipelineId) return;

        this.designer.execStatusArea.innerHTML = `
            <div class="alert alert-info">
                <div class="d-flex align-items-center">
                    <div class="spinner-border spinner-border-sm me-2" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                    <div>Starting execution...</div>
                </div>
            </div>`;

        if (window.pipelineDesigner && typeof window.pipelineDesigner.resetAllNodeStatus === 'function') {
            window.pipelineDesigner.resetAllNodeStatus();
        }

        try {
            const resp = await fetch(`rest/platform/agent/admin/pipelines/${this.designer.pipelineId}/execute`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(inputs)
            });

            if (!resp.ok) {
                throw new Error(`HTTP error: ${resp.status}`);
            }

            const data = await resp.json();
            this._updateExecStatus('Running', 'primary', data.result.executionId);
            this._connectToPipelineSSE(data.result.executionId);
        } catch (e) {
            this._updateExecStatus('Error', 'danger');
            this._addExecEventMessage(`Execution error: ${e.message}`, 'danger');
        }
    }

    _connectToPipelineSSE(executionId) {
        if (this.sseConnection) {
            this.sseConnection.close();
            this.sseConnection = null;
        }

        const eventsArea = this.designer.execStatusArea;
        eventsArea.innerHTML = '';

        const sseUrl = `rest/platform/agent/admin/pipelines/executions/${executionId}/events`;
        this.sseConnection = new EventSource(sseUrl);

        this._updateExecStatus('Running', 'primary', executionId);
        this._addExecEventMessage('Connexion SSE établie', 'info');

        this.sseConnection.onopen = () => {
            this._addExecEventMessage('Connexion SSE ouverte', 'info');
        };

        this.sseConnection.onerror = () => {
            this._addExecEventMessage('Erreur de connexion SSE', 'danger');
            this._updateExecStatus('Error', 'danger', executionId);
            this.sseConnection.close();
            this.sseConnection = null;
        };

        const eventTypes = [
            'PIPELINE_START', 'PIPELINE_NODE_START', 'PIPELINE_NODE_SUCCESS',
            'PIPELINE_NODE_FAILED', 'PIPELINE_NODE_STREAM', 'PIPELINE_PORT_CHOSEN',
            'PIPELINE_COMPLETE', 'PIPELINE_FAILED'
        ];

        eventTypes.forEach(eventType => {
            this.sseConnection.addEventListener(eventType, (event) => {
                try {
                    const eventData = JSON.parse(event.data);
                    this._handlePipelineExecEvent(eventData);
                } catch (e) {
                    console.error(`Event parsing error for ${eventType}:`, e);
                    this._addExecEventMessage(`Erreur d'analyse de l'événement : ${e.message}`, 'warning');
                }
            });
        });

        this.sseConnection.addEventListener('message', (event) => {
            try {
                const eventData = JSON.parse(event.data);
                this._handlePipelineExecEvent(eventData);
            } catch (e) {
                console.error('Generic event parsing error:', e);
                this._addExecEventMessage(`Erreur d'analyse de l'événement : ${e.message}`, 'warning');
            }
        });
    }

    _handlePipelineExecEvent(event) {
        console.log('Pipeline event received:', event);
        
        let badgeClass = 'dark';
        const eventType = event.eventType;
        const payload = event.payload;

        switch (eventType) {
            case 'PIPELINE_START':
                badgeClass = 'primary';
                this._updateExecStatus('Running', 'primary');
                setTimeout(() => this.designer.renderManager.centerView(), 300);
                break;

            case 'PIPELINE_NODE_START':
            case 'NODE_START':
                badgeClass = 'info';
                if (payload?.nodeId) {
                    this.designer.nodeManager.updateNodeStatus(payload.nodeId, 'running', payload.message);
                    this._followNode(payload.nodeId);
                }
                break;

            case 'PIPELINE_NODE_SUCCESS':
            case 'NODE_SUCCESS':
                badgeClass = 'success';
                if (payload?.nodeId) {
                    this.designer.nodeManager.updateNodeStatus(payload.nodeId, 'success', payload.message);
                }
                break;

            case 'PIPELINE_NODE_FAILED':
                badgeClass = 'danger';
                if (payload?.nodeId) {
                    this.designer.nodeManager.updateNodeStatus(payload.nodeId, 'error', payload.error || payload.message);
                    this._followNode(payload.nodeId, { zoom: 2, duration: 1000 });
                }
                break;

            case 'PIPELINE_NODE_STREAM':
            case 'NODE_STREAM':
                badgeClass = 'warning';
                if (payload?.nodeId) {
                    const streamMessage = payload.data?.token || 'Streaming...';
                    this.designer.nodeManager.updateNodeStatus(payload.nodeId, 'running', streamMessage);
                    const node = this.designer.findNodeByDataId(payload.nodeId);
                    if (node) {
                        node.el.setAttribute('data-streaming', 'true');
                        this._followNode(payload.nodeId, { zoom: 1.3 });
                    }
                }
                break;

            case 'PIPELINE_PORT_CHOSEN':
            case 'PORT_CHOSEN':
                if (payload?.nodeId && payload?.outputPort) {
                    const node = this.designer.findNodeByDataId(payload.nodeId) || this.designer.nodesById[payload.nodeId];
                    if (node) {
                        const outgoingEdges = this.designer.edges.filter(e => 
                            (e.source === node.id && e.sourcePort === payload.outputPort)
                        );
                        if (outgoingEdges && outgoingEdges.length) {
                            outgoingEdges.forEach(edge => {
                                this.designer.edgeManager.updateEdgeStatus(edge.source, edge.target, true);
                                const targetNode = this.designer.nodesById[edge.target];
                                if (targetNode) {
                                    setTimeout(() => {
                                        this._followNode(edge.target);
                                    }, 500);
                                }
                            });
                        }
                    }
                }
                break;

            case 'PIPELINE_COMPLETE':
                badgeClass = 'success';
                this._updateExecStatus('Completed', 'success');
                this._addExecEventMessage('Pipeline completed', 'success');
                this._showExecOutput(payload?.outputs || payload?.data);
                this.designer.uiManager.pipelineExecStatusModal.show();
                setTimeout(() => {
                    this.designer.renderManager.centerView();
                }, 1800);
                if (this.sseConnection) {
                    this.sseConnection.close();
                    this.sseConnection = null;
                }
                break;

            case 'PIPELINE_FAILED':
                badgeClass = 'danger';
                this._updateExecStatus('Failed', 'danger');
                this._addExecEventMessage('Pipeline failed', 'danger');
                this._showExecOutput({ error: payload?.error || payload?.message });
                this.designer.uiManager.pipelineExecStatusModal.show();
                if (this.sseConnection) {
                    this.sseConnection.close();
                    this.sseConnection = null;
                }
                break;

            default:
                if (payload?.nodeId) {
                    this._followNode(payload.nodeId);
                }
                break;
        }

        // Générer un message plus informatif basé sur l'événement
        const message = this._generateEventMessage(eventType, payload);
        this._addExecEventMessage(message, badgeClass, event);
    }

    _generateEventMessage(eventType, payload) {
        switch (eventType) {
            case 'PIPELINE_START':
                return 'Pipeline démarré avec succès';

            case 'PIPELINE_NODE_START':
            case 'NODE_START':
                if (payload?.nodeType && payload?.nodeId) {
                    return `Nœud ${payload.nodeType} (${payload.nodeId}) - Démarrage`;
                }
                return 'Démarrage d\'un nœud';

            case 'PIPELINE_NODE_SUCCESS':
            case 'NODE_SUCCESS':
                if (payload?.nodeType && payload?.nodeId) {
                    const outputCount = payload.outputs ? Object.keys(payload.outputs).length : 0;
                    return `Nœud ${payload.nodeType} (${payload.nodeId}) - Succès${outputCount > 0 ? ` ( + ${outputCount} variable${outputCount > 1 ? 's' : ''})` : ''}`;
                }
                return 'Nœud exécuté avec succès';

            case 'PIPELINE_NODE_FAILED':
                if (payload?.nodeType && payload?.nodeId) {
                    const errorMsg = payload.error || payload.message || 'Erreur inconnue';
                    return `Nœud ${payload.nodeType} (${payload.nodeId}) - Erreur: ${errorMsg}`;
                }
                return `Erreur lors de l'exécution d'un nœud: ${payload?.error || payload?.message || 'Erreur inconnue'}`;

            case 'PIPELINE_NODE_STREAM':
            case 'NODE_STREAM':
                if (payload?.nodeType && payload?.nodeId) {
                    const token = payload.data?.token || '';
                    const truncatedToken = token.length > 50 ? token.substring(0, 47) + '...' : token;
                    return `Nœud ${payload.nodeType} (${payload.nodeId}) - Stream: ${truncatedToken}`;
                }
                return 'Données en streaming';

            case 'PIPELINE_PORT_CHOSEN':
            case 'PORT_CHOSEN':
                if (payload?.nodeType && payload?.nodeId && payload?.outputPort) {
                    return `Nœud ${payload.nodeType} (${payload.nodeId}) - Port de sortie sélectionné: ${payload.outputPort}`;
                }
                return 'Port de sortie sélectionné';

            case 'PIPELINE_COMPLETE':
                const outputCount = payload?.outputs ? Object.keys(payload.outputs).length : 0;
                return `Pipeline terminé avec succès${outputCount > 0 ? ` (${outputCount} variable${outputCount > 1 ? 's' : ''})` : ''}`;

            case 'PIPELINE_FAILED':
                return `Pipeline échoué: ${payload?.error || payload?.message || 'Erreur inconnue'}`;

            default:
                return payload?.message || `Événement: ${eventType}`;
        }
    }

    _followNode(nodeId, options = {}) {
        if (!this.designer._autoFollow) return;

        if (this._animating) {
            this._pendingFollow = { nodeId, options };
            return;
        }

        this._animating = true;

        const node = this.designer.nodesById[nodeId] || this.designer.findNodeByDataId(nodeId);
        if (!node) {
            this._animating = false;
            return;
        }

        const defaults = { zoom: 1.5, duration: 800, padding: 100 };
        const opts = { ...defaults, ...options };
        
        if (opts.zoom > 1.3) opts.zoom = 1.1;

        const rect = this.designer.container.getBoundingClientRect();
        const centerX = rect.width / 2;
        const centerY = rect.height / 2;

        const nodeCenterX = (node.x + node.el.offsetWidth / 2) * opts.zoom;
        const nodeCenterY = (node.y + node.el.offsetHeight / 2) * opts.zoom;

        const targetPanX = centerX - nodeCenterX;
        const targetPanY = centerY - nodeCenterY;

        const startZoom = this.designer.renderManager._zoomLevel;
        const startPanX = this.designer.renderManager._panX;
        const startPanY = this.designer.renderManager._panY;
        const startTime = performance.now();

        const easeOutCubic = t => 1 - Math.pow(1 - t, 3);

        const animate = now => {
            const p = Math.min(1, (now - startTime) / opts.duration);
            const k = easeOutCubic(p);

            this.designer.renderManager._zoomLevel = startZoom + (opts.zoom - startZoom) * k;
            this.designer.renderManager._panX = startPanX + (targetPanX - startPanX) * k;
            this.designer.renderManager._panY = startPanY + (targetPanY - startPanY) * k;

            this.designer.renderManager._updateTransform();

            if (p < 1) {
                this._animFrameId = requestAnimationFrame(animate);
            } else {
                this._animFrameId = null;
                this._animating = false;
                if (this._pendingFollow) {
                    const { nodeId: nextId, options: nextOpts } = this._pendingFollow;
                    this._pendingFollow = null;
                    this._followNode(nextId, nextOpts);
                }
            }
        };

        this._animFrameId = requestAnimationFrame(animate);
    }

    _addExecEventMessage(message, badgeType = 'secondary', event = null) {
        const eventsArea = this.designer.execStatusArea;
        const eventItem = document.createElement('div');
        eventItem.className = 'exec-event-item';

        let badgeText = event ? event.eventType : 'INFO';
        const timestamp = new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit', second:'2-digit'});

        eventItem.innerHTML = `
            <div class="event-header">
                <span class="event-time">${timestamp}</span>
                <span class="event-badge badge bg-${badgeType}-subtle text-${badgeType}-emphasis">${badgeText}</span>
            </div>
            <div class="event-content">${message}</div>`;

        eventsArea.appendChild(eventItem);
        eventsArea.scrollTop = eventsArea.scrollHeight;
    }

    _updateExecStatus(status, color, execId = null) {
        const execHeader = document.createElement('div');
        execHeader.className = 'exec-status-header';
        execHeader.innerHTML = `
            <div class="d-flex align-items-center">
                <div class="me-2">
                    <span class="badge bg-${color}-subtle text-${color}-emphasis px-3 py-2">${status}</span>
                </div>
                ${execId ? `<div class="text-muted small">ID: ${execId}</div>` : ''}
            </div>`;

        const oldHeader = this.designer.execStatusArea.querySelector('.exec-status-header');
        if (oldHeader) {
            oldHeader.remove();
        }

        if (this.designer.execStatusArea.firstChild) {
            this.designer.execStatusArea.insertBefore(execHeader, this.designer.execStatusArea.firstChild);
        } else {
            this.designer.execStatusArea.appendChild(execHeader);
        }
    }

    _showExecOutput(data) {
        const oldResult = this.designer.execStatusArea.querySelector('.execution-result');
        if (oldResult) {
            oldResult.remove();
        }

        const outDiv = document.createElement('div');
        outDiv.className = 'execution-result';

        const resultHeader = document.createElement('div');
        resultHeader.className = 'execution-result-header';
        resultHeader.innerHTML = `
            <h6 class="fw-bold mb-2">
                <i class="ti ti-clipboard-text me-2"></i>Résultat de l'exécution
            </h6>`;

        outDiv.appendChild(resultHeader);

        if (data && typeof data === 'object' && !Array.isArray(data)) {
            Object.entries(data).forEach(([key, value]) => {
                const responseSection = document.createElement('div');
                responseSection.className = 'response-section mb-4';

                const sectionHeader = document.createElement('div');
                sectionHeader.className = 'response-section-header d-flex align-items-center mb-2';
                sectionHeader.innerHTML = `
                    <i class="ti ti-file-text me-2 text-primary"></i>
                    <h6 class="mb-0 fw-semibold">${key}</h6>`;

                const sectionContent = document.createElement('div');
                sectionContent.className = 'response-section-content';

                if (typeof value === 'string') {
                    const markdownContent = document.createElement('div');
                    markdownContent.className = 'markdown-content';
                    if (typeof marked !== 'undefined') {
                        markdownContent.innerHTML = marked.parse(value);
                    } else {
                        markdownContent.innerHTML = `<p>${value.replace(/\n/g, '<br>')}</p>`;
                    }
                    sectionContent.appendChild(markdownContent);
                } else {
                    const codeBlock = document.createElement('pre');
                    codeBlock.className = 'code-block p-3 rounded';
                    codeBlock.textContent = JSON.stringify(value, null, 2);
                    sectionContent.appendChild(codeBlock);
                }

                responseSection.appendChild(sectionHeader);
                responseSection.appendChild(sectionContent);
                outDiv.appendChild(responseSection);
            });
        } else {
            const codeBlock = document.createElement('pre');
            codeBlock.className = 'code-block p-3 rounded';
            codeBlock.textContent = JSON.stringify(data, null, 2);
            outDiv.appendChild(codeBlock);
        }

        this.designer.execStatusArea.appendChild(outDiv);
    }
}