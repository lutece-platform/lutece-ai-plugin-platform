import { fieldHTML } from './utils.js';

export default class UIManager {
  constructor(pipelineDesigner) {
    this.designer = pipelineDesigner;
    this._currentEditingNode = null;
    this.customVariables = new Set();
    this.activeFieldVariables = new Map();
    this.tribute = null;
    this.availableOutputKeys = [];
    this._initNodeConfigModal();
    this._initNodeMenu();
    this._initPipelineEditModal();
    this._initPipelineExecModal();
    this._initPipelineExecStatusModal();
  }

  _initNodeConfigModal(opts = {}) {
    let modalEl = document.getElementById('nodeConfigModal');
    if (!modalEl) {
      modalEl = document.createElement('div');
      modalEl.className = 'modal fade';
      modalEl.id = 'nodeConfigModal';
      modalEl.tabIndex = -1;
      modalEl.setAttribute('aria-labelledby', 'nodeConfigModalLabel');
      modalEl.setAttribute('aria-hidden', 'true');
      modalEl.innerHTML = `
                <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title fw-bold" id="nodeConfigModalLabel">Configuration du nœud</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
                        </div>
                        <div class="modal-body">
                            <div class="alert alert-info" id="nodeConfigVariableHint">
                                Tapez <kbd>{{</kbd> dans un champ texte pour insérer une variable. Les variables d'entrée sont définies dans le nœud Départ.
                            </div>
                            <div id="${opts.configId || 'modal-node-config'}" >
                                <div class="d-flex align-items-center justify-content-center py-5">
                                    <div class="spinner-border text-primary me-3" role="status">
                                        <span class="visually-hidden">Chargement...</span>
                                    </div>
                                    <p class="text-muted mb-0">Chargement des options de configuration...</p>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Annuler</button>
                            <button type="button" class="btn btn-primary" id="saveNodeConfig">Enregistrer la configuration</button>
                        </div>
                    </div>
                </div>`;
      document.body.appendChild(modalEl);
    }

    this.modalEl = modalEl;
    this.cfgEl = document.getElementById(opts.configId || 'modal-node-config');
    this.nodeConfigModal = this.modalEl ? new bootstrap.Modal(this.modalEl) : null;

    document.getElementById('saveNodeConfig')?.addEventListener('click', () => {
      if (this._currentEditingNode) {
        if (this._currentEditingNode.type.toUpperCase() === 'START') {
          this._saveStartNodeConfig();
        } else {
          this._saveNodeConfig();
        }
        this.nodeConfigModal.hide();
      }
    });

    this.modalEl?.addEventListener('hidden.bs.modal', () => {
      this._currentEditingNode = null;
    });
  }

  _initNodeMenu() {
    this.nodeMenuEl = document.createElement('div');
    this.nodeMenuEl.className = 'pf-node-menu';
    this.nodeMenuEl.style.display = 'none';
    document.body.appendChild(this.nodeMenuEl);

    document.addEventListener('click', (e) => {
      if (!e.target.closest('.pf-node-menu')) {
        this.nodeMenuEl.style.display = 'none';
      }
    });
  }

  _showNodeTypeMenu(x, y, clientX, clientY, sourceNodeId = null, sourcePort = null) {
    this.nodeMenuEl.innerHTML = '';
    this.nodeMenuEl.style.display = 'block';

    const menuHeader = document.createElement('div');
    menuHeader.className = 'pf-node-menu-header';

    const searchWrapper = document.createElement('div');
    searchWrapper.className = 'search-wrapper px-3 py-2';

    const searchInput = document.createElement('input');
    searchInput.type = 'text';
    searchInput.className = 'form-control form-control-sm';
    searchInput.placeholder = 'Rechercher des nœuds...';
    searchInput.addEventListener('input', e => {
      const query = e.target.value.toLowerCase().trim();
      document.querySelectorAll('.node-menu-item').forEach(item => {
        const type = item.textContent.toLowerCase();
        const match = type.includes(query);
        item.style.display = match ? '' : 'none';
      });

      document.querySelectorAll('.node-menu-category').forEach(cat => {
        const hasVisibleItems = Array.from(cat.querySelectorAll('.node-menu-item'))
          .some(item => item.style.display !== 'none');
        cat.style.display = hasVisibleItems ? '' : 'none';
      });
    });

    searchWrapper.appendChild(searchInput);
    menuHeader.appendChild(searchWrapper);
    this.nodeMenuEl.appendChild(menuHeader);

    const menuContent = document.createElement('div');
    menuContent.className = 'pf-node-menu-content';
    this.nodeMenuEl.appendChild(menuContent);

    const categories = {};
    Object.entries(this.designer.nodeTypes).forEach(([type, info]) => {
      const category = info.category || 'Uncategorized';
      if (!categories[category]) {
        categories[category] = [];
      }
      categories[category].push({ type, info });
    });

    Object.entries(categories).sort().forEach(([category, nodes]) => {
      const categoryEl = document.createElement('div');
      categoryEl.className = 'node-menu-category';

      nodes.sort((a, b) => a.type.localeCompare(b.type)).forEach(({ type, info }) => {
        const item = document.createElement('a');
        item.className = 'dropdown-item node-menu-item';
        item.href = '#';

        const icon = document.createElement('span');
        icon.className = 'me-2 text-muted';
        icon.innerHTML = info.icon || '<i class="ti ti-box"></i>';

        const label = document.createElement('span');
        label.className = 'node-menu-label';
        label.innerHTML = `<span class="fw-bold">${this.designer._getNodeTypeInfo(type).name || type}</span>
                                  <span class="d-block small text-muted">${this.designer._getNodeTypeInfo(type).description || ''}</span>`;

        item.appendChild(icon);
        item.appendChild(label);

        item.addEventListener('click', e => {
          e.preventDefault();

          if (sourceNodeId && sourcePort) {
            const sourceNode = this.designer.nodesById[sourceNodeId];
            x = sourceNode.x + sourceNode.el.offsetWidth + 50;
            y = sourceNode.y;
          }

          const nodeId = this.designer.addNode(type, { x, y });

          if (sourceNodeId && sourcePort) {
            const node = this.designer.nodesById[nodeId];
            const inputPorts = Object.keys(this.designer.nodeTypes[type]?.inputPorts || { input: 'Default input port' });
            if (inputPorts.length > 0) {
              this.designer.addEdge({
                source: sourceNodeId,
                sourcePort: sourcePort,
                target: nodeId,
                targetPort: inputPorts[0]
              });
            }
          }

          this.nodeMenuEl.style.display = 'none';
        });

        categoryEl.appendChild(item);
      });

      menuContent.appendChild(categoryEl);
    });

    const rect = this.designer.container.getBoundingClientRect();
    this.nodeMenuEl.style.top = `${clientY}px`;
    this.nodeMenuEl.style.left = `${clientX}px`;

    const menuRect = this.nodeMenuEl.getBoundingClientRect();
    if (menuRect.right > window.innerWidth) {
      this.nodeMenuEl.style.left = `${clientX - menuRect.width}px`;
    }
    if (menuRect.bottom > window.innerHeight) {
      const newTop = clientY - menuRect.height;
      if (newTop < 0) {
        this.nodeMenuEl.style.top = '0px';
        this.nodeMenuEl.style.maxHeight = '500px';
      } else {
        this.nodeMenuEl.style.top = `${newTop}px`;
      }
    }

    setTimeout(() => searchInput.focus(), 100);
  }

  _initPipelineEditModal() {
    let editModalEl = document.getElementById('pipelineEditModal');
    if (!editModalEl) {
      editModalEl = document.createElement('div');
      editModalEl.className = 'modal fade';
      editModalEl.id = 'pipelineEditModal';
      editModalEl.tabIndex = -1;
      editModalEl.setAttribute('aria-labelledby', 'pipelineEditModalLabel');
      editModalEl.setAttribute('aria-hidden', 'true');
      editModalEl.innerHTML = `
                <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title fw-bold" id="pipelineEditModalLabel">Modifier le pipeline</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
                        </div>
                        <div class="modal-body">
                            <div id="pipeline-edit-form-container">
                                <div class="d-flex align-items-center justify-content-center py-5">
                                    <div class="spinner-border text-primary me-3" role="status">
                                        <span class="visually-hidden">Chargement...</span>
                                    </div>
                                    <p class="text-muted mb-0">Chargement des informations du pipeline...</p>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Annuler</button>
                            <button type="button" class="btn btn-primary" id="savePipelineBtn">Enregistrer le pipeline</button>
                        </div>
                    </div>
                </div>`;
      document.body.appendChild(editModalEl);
    }

    this.editModalEl = editModalEl;
    this.editFormContainer = document.getElementById('pipeline-edit-form-container');
    this.pipelineEditModal = this.editModalEl ? new bootstrap.Modal(this.editModalEl) : null;

    document.getElementById('savePipelineBtn')?.addEventListener('click', () => {
      this._submitPipelineEditForm();
    });
  }

  _initPipelineExecModal() {
    let execModalEl = document.getElementById('pipelineExecModal');
    if (!execModalEl) {
      execModalEl = document.createElement('div');
      execModalEl.className = 'modal fade';
      execModalEl.id = 'pipelineExecModal';
      execModalEl.tabIndex = -1;
      execModalEl.setAttribute('aria-labelledby', 'pipelineExecModalLabel');
      execModalEl.setAttribute('aria-hidden', 'true');
      execModalEl.innerHTML = `
                <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title fw-bold" id="pipelineExecModalLabel">Exécuter le pipeline</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
                        </div>
                        <div class="modal-body">
                            <div id="pipeline-exec-form-container">
                                <div class="d-flex align-items-center justify-content-center py-5">
                                    <div class="spinner-border text-primary me-3" role="status">
                                        <span class="visually-hidden">Chargement...</span>
                                    </div>
                                    <p class="text-muted mb-0">Chargement des paramètres requis...</p>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Annuler</button>
                            <button type="button" class="btn btn-primary" id="runPipelineBtn">Exécuter le pipeline</button>
                        </div>
                    </div>
                </div>`;
      document.body.appendChild(execModalEl);
    }

    this.execModalEl = execModalEl;
    this.execFormContainer = document.getElementById('pipeline-exec-form-container');
    this.pipelineExecModal = this.execModalEl ? new bootstrap.Modal(this.execModalEl) : null;

    document.getElementById('runPipelineBtn')?.addEventListener('click', () => {
      this._submitPipelineExecForm();
    });
  }

  _renderConfig(node) {
    this._currentEditingNode = node;
    if (!this.cfgEl) return;

    if (!node) {
      this.cfgEl.innerHTML = `
                <div class="text-center py-5">
                    <i class="ti ti-click fs-2 text-muted mb-3"></i>
                    <p class="text-muted">Sélectionnez un nœud pour éditer sa configuration</p>
                </div>`;
      return;
    }

    const hint = document.getElementById('nodeConfigVariableHint');

    const saveBtn = document.getElementById('saveNodeConfig');

    if (node.type.toUpperCase() === 'START') {
      if (hint) hint.style.display = 'none';
      this._renderStartNodeConfig();
      return;
    }

    if (hint) hint.style.display = '';

    const nodeTypeInfo = this.designer.nodeTypes[node.type] || {};
    const vars = nodeTypeInfo.variables || {};
    const data = node.data || {};

    const html = [];
    const categories = {};
    Object.entries(vars).forEach(([k, v]) => {
      const category = v.category || 'Général';
      if (!categories[category]) {
        categories[category] = [];
      }
      categories[category].push([k, v]);
    });

    if (Object.keys(categories).length > 0) {
      Object.entries(categories).forEach(([category, fields]) => {
        html.push(`<div class="config-section">
                    <div class="config-section-content">`);
        fields.forEach(([k, v]) => {
          html.push(fieldHTML(k, v, data[k], { inputSchema: this.designer.inputSchema }));
        });
        html.push(`</div></div>`);
      });
    } else {
      html.push(`<em>
                Ce type de nœud n'a pas de propriétés configurables.
            </em>`);
    }

    if (nodeTypeInfo.outputKeys && Object.keys(nodeTypeInfo.outputKeys).length > 0) {
      html.push(`
                <div class="config-section">
                    <h6 class="config-section-title">Sorties disponibles</h6>
                    <div class="config-section-content">
                        <div class="table-responsive">
                            <table class="table">
                                <thead>
                                    <tr>
                                        <th>Clé</th>
                                        <th>Description</th>
                                    </tr>
                                </thead>
                                <tbody>`);

      Object.entries(nodeTypeInfo.outputKeys).forEach(([key, description]) => {
        if (key === "__dynamic__") {
          html.push(`
                        <tr>
                            <td><span class="badge bg-light text-dark">Variable dynamique</span></td>
                            <td>${description}</td>
                        </tr>`);
        } else {
          html.push(`
                        <tr>
                            <td><code class="node-output-key">${node.id}.${key}</code></td>
                            <td>${description}</td>
                        </tr>`);
        }
      });

      html.push(`
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>`);
    }

    this.cfgEl.innerHTML = html.join('');

    this.cfgEl.querySelectorAll('.add-object-field').forEach(btn => {
      btn.addEventListener('click', () => {
        const objectField = btn.closest('.object-field');
        const fieldName = objectField.dataset.field;
        const entriesContainer = objectField.querySelector('.object-entries');
        const template = objectField.querySelector('.object-entry-template');

        const newEntryWrapper = document.createElement('div');
        newEntryWrapper.innerHTML = template.innerHTML;
        const newEntry = newEntryWrapper.firstElementChild;

        entriesContainer.appendChild(newEntry);

        newEntry.querySelector('.remove-object-item').addEventListener('click', () => {
          newEntry.remove();
        });
      });
    });

    this.cfgEl.querySelectorAll('.remove-object-item').forEach(btn => {
      btn.addEventListener('click', () => {
        btn.closest('.object-entry').remove();
      });
    });

    const modalTitle = document.getElementById('nodeConfigModalLabel');
    if (modalTitle) {
      modalTitle.innerHTML = `
                <span>${nodeTypeInfo.name || node.type}</span>
                <p class="text-muted small mb-0">${nodeTypeInfo.description || ''}</p>
            `;
    }

    this.nodeConfigModal.show();

    setTimeout(() => {
      this._updateAvailableOutputKeys();
      if (this.tribute) {
        this.tribute.collection[0].values = this.availableOutputKeys;
      }
      this._attachTributeToFields();
    }, 100);
  }

  _renderStartNodeConfig() {
    const schema = this.designer.inputSchema || [];
    const typeLabels = { STRING: 'Texte', NUMBER: 'Nombre', BOOLEAN: 'Oui/Non', ENUM: 'Liste de choix', FILE: 'Fichier' };
    const typeIcons = { STRING: 'ti-forms', NUMBER: 'ti-123', BOOLEAN: 'ti-toggle-left', ENUM: 'ti-list', FILE: 'ti-file-upload' };

    const renderRow = (field, idx) => {
      const typeIcon = typeIcons[field.type] || 'ti-forms';
      const typeLabel = typeLabels[field.type] || field.type;
      const badges = [];
      if (field.required) badges.push('<span class="badge text-bg-danger">requis</span>');
      if (field.multiple) badges.push('<span class="badge text-bg-secondary">multiple</span>');
      if (field.isTextarea) badges.push('<span class="badge text-bg-secondary">multiligne</span>');

      return `<tr class="input-schema-field" data-index="${idx}">
        <td class="fw-medium">${field.title || field.name}</td>
        <td class="text-muted small">${field.description || '-'}</td>
        <td><span class="d-inline-flex align-items-center gap-1"><i class="ti ${typeIcon}"></i> ${typeLabel}</span></td>
        <td><div class="d-flex flex-wrap gap-1">${badges.join('') || '<span class="text-muted small">-</span>'}</div></td>
        <td class="text-end text-nowrap">
          <button type="button" class="btn btn-sm btn-light edit-schema-field" data-index="${idx}" title="Modifier"><i class="ti ti-pencil"></i></button>
          <button type="button" class="btn btn-sm btn-light text-danger remove-schema-field" data-index="${idx}" title="Supprimer"><i class="ti ti-trash"></i></button>
        </td>
      </tr>`;
    };

    if (schema.length > 0) {
      this.cfgEl.innerHTML = `
        <div class="d-flex justify-content-end mb-2">
          <button type="button" class="btn btn-sm btn-primary" id="add-schema-field"><i class="ti ti-plus me-1"></i>Ajouter un champ</button>
        </div>
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead><tr><th>Champ</th><th>Description</th><th>Type</th><th>Options</th><th></th></tr></thead>
            <tbody>${schema.map((f, i) => renderRow(f, i)).join('')}</tbody>
          </table>
        </div>`;
    } else {
      this.cfgEl.innerHTML = `
        <div class="text-center py-4">
          <i class="ti ti-forms fs-1 text-muted d-block mb-2"></i>
          <p class="text-muted mb-2">Ce pipeline n'a pas encore de formulaire d'entree.</p>
          <p class="text-muted small mb-3">Les champs definis ici seront presentes a l'utilisateur lors de l'execution.</p>
          <button type="button" class="btn btn-sm btn-primary" id="add-schema-field"><i class="ti ti-plus me-1"></i>Ajouter un premier champ</button>
        </div>`;
    }

    const modalTitle = document.getElementById('nodeConfigModalLabel');
    if (modalTitle) {
      modalTitle.innerHTML = `<span>Formulaire d'entree</span>
        <p class="text-muted small mb-0">Champs presentes a l'utilisateur lors de l'execution du pipeline</p>`;
    }

    this._currentEditingNode = this._currentEditingNode || Object.values(this.designer.nodesById).find(n => n.type.toUpperCase() === 'START');
    this.nodeConfigModal.show();
    this._schemaData = [...schema];

    const self = this;
    this.cfgEl.querySelectorAll('.remove-schema-field').forEach(btn => {
      btn.onclick = () => {
        self._schemaData.splice(parseInt(btn.dataset.index), 1);
        self._commitSchema();
        self._renderStartNodeConfig();
      };
    });
    this.cfgEl.querySelectorAll('.edit-schema-field').forEach(btn => {
      btn.onclick = () => self._openSchemaFieldModal(parseInt(btn.dataset.index));
    });
    document.getElementById('add-schema-field')?.addEventListener('click', () => self._openSchemaFieldModal(-1));
  }

  _openSchemaFieldModal(idx) {
    this.nodeConfigModal.hide();

    const isNew = idx === -1;
    const field = isNew ? { type: 'STRING', required: true } : { ...this._schemaData[idx] };
    const typeLabels = { STRING: 'Texte', NUMBER: 'Nombre', BOOLEAN: 'Oui/Non', ENUM: 'Liste de choix', FILE: 'Fichier' };

    const renderTypeExtras = (t, f) => {
      if (t === 'STRING') return `
        <div class="mb-3">
          <label class="form-label">Placeholder</label>
          <input type="text" class="form-control" id="sf_placeholder" value="${f.placeholder || ''}">
        </div>
        <div class="form-check mb-3">
          <input class="form-check-input" type="checkbox" id="sf_textarea" ${f.isTextarea ? 'checked' : ''}>
          <label class="form-check-label" for="sf_textarea">Champ multiligne</label>
        </div>`;
      if (t === 'FILE') return `
        <div class="form-check mb-3">
          <input class="form-check-input" type="checkbox" id="sf_multiple" ${f.multiple ? 'checked' : ''}>
          <label class="form-check-label" for="sf_multiple">Autoriser plusieurs fichiers</label>
        </div>`;
      if (t === 'ENUM') {
        const options = (f.enumOptions || []);
        const rows = options.map((o, i) => `
          <div class="input-group input-group-sm mb-1 sf-enum-row">
            <input type="text" class="form-control" placeholder="Valeur" value="${o.value || ''}">
            <input type="text" class="form-control" placeholder="Label (optionnel)" value="${o.label && o.label !== o.value ? o.label : ''}">
            <button type="button" class="btn btn-light text-danger sf-enum-remove"><i class="ti ti-x"></i></button>
          </div>`).join('');
        return `
          <div class="mb-3">
            <label class="form-label">Options</label>
            <div id="sf_enum_options">${rows}</div>
            <button type="button" class="btn btn-sm btn-light mt-1" id="sf_enum_add"><i class="ti ti-plus me-1"></i>Ajouter une option</button>
          </div>`;
      }
      if (t === 'NUMBER') return `
        <div class="row g-3 mb-3">
          <div class="col-6">
            <label class="form-label">Minimum</label>
            <input type="number" class="form-control" id="sf_min" value="${f.minValue ?? ''}">
          </div>
          <div class="col-6">
            <label class="form-label">Maximum</label>
            <input type="number" class="form-control" id="sf_max" value="${f.maxValue ?? ''}">
          </div>
        </div>`;
      return '';
    };

    let modal = document.getElementById('schemaFieldModal');
    if (modal) modal.remove();

    const html = `
      <div class="modal fade" id="schemaFieldModal" tabindex="-1" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">${isNew ? 'Ajouter un champ' : 'Modifier le champ'}</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
              <div class="row g-3 mb-3">
                <div class="col-7">
                  <label class="form-label">Titre <span class="text-danger">*</span></label>
                  <input type="text" class="form-control" id="sf_title" value="${field.title || ''}" placeholder="ex: Nom du client">
                </div>
                <div class="col-5">
                  <label class="form-label">Cle technique <span class="text-danger">*</span></label>
                  <input type="text" class="form-control font-monospace" id="sf_name" value="${field.name || ''}" placeholder="nom_client">
                  <div class="form-text">a-z, 0-9, _</div>
                </div>
              </div>
              <div class="row g-3 mb-3">
                <div class="col-7">
                  <label class="form-label">Type</label>
                  <select class="form-select" id="sf_type">
                    ${Object.entries(typeLabels).map(([v, l]) => `<option value="${v}" ${field.type === v ? 'selected' : ''}>${l}</option>`).join('')}
                  </select>
                </div>
                <div class="col-5 d-flex align-items-end pb-1">
                  <div class="form-check">
                    <input class="form-check-input" type="checkbox" id="sf_required" ${field.required ? 'checked' : ''}>
                    <label class="form-check-label" for="sf_required">Requis</label>
                  </div>
                </div>
              </div>
              <div class="mb-3">
                <label class="form-label">Description <span class="text-muted small">(optionnel)</span></label>
                <input type="text" class="form-control" id="sf_description" value="${field.description || ''}" placeholder="Texte d'aide affiche sous le champ">
              </div>
              <div id="sf_type_extras">${renderTypeExtras(field.type || 'STRING', field)}</div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-light" data-bs-dismiss="modal">Annuler</button>
              <button type="button" class="btn btn-primary" id="sf_save">${isNew ? 'Ajouter' : 'Enregistrer'}</button>
            </div>
          </div>
        </div>
      </div>`;

    document.body.insertAdjacentHTML('beforeend', html);
    modal = document.getElementById('schemaFieldModal');
    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();

    const nameInput = modal.querySelector('#sf_name');
    nameInput.addEventListener('input', () => {
      nameInput.value = nameInput.value.toLowerCase().replace(/[^a-z0-9_]/g, '');
    });

    modal.querySelector('#sf_type').addEventListener('change', (e) => {
      modal.querySelector('#sf_type_extras').innerHTML = renderTypeExtras(e.target.value, {});
      this._attachEnumEvents(modal);
    });
    this._attachEnumEvents(modal);

    modal.querySelector('#sf_save').addEventListener('click', () => {
      const title = modal.querySelector('#sf_title').value.trim();
      const name = modal.querySelector('#sf_name').value.trim();

      modal.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));

      if (!title) { modal.querySelector('#sf_title').classList.add('is-invalid'); return; }
      if (!name || !/^[a-z][a-z0-9_]*$/.test(name)) { modal.querySelector('#sf_name').classList.add('is-invalid'); return; }

      const duplicate = this._schemaData.some((f, i) => f.name === name && i !== idx);
      if (duplicate) {
        const nameEl = modal.querySelector('#sf_name');
        nameEl.classList.add('is-invalid');
        let feedback = nameEl.parentElement.querySelector('.invalid-feedback');
        if (!feedback) {
          feedback = document.createElement('div');
          feedback.className = 'invalid-feedback';
          nameEl.parentElement.appendChild(feedback);
        }
        feedback.textContent = 'Cette cle existe deja';
        return;
      }

      const type = modal.querySelector('#sf_type').value;
      const result = { name, title, type, required: modal.querySelector('#sf_required').checked };
      const desc = modal.querySelector('#sf_description').value.trim();
      if (desc) result.description = desc;

      if (type === 'STRING') {
        const ph = modal.querySelector('#sf_placeholder')?.value?.trim();
        if (ph) result.placeholder = ph;
        if (modal.querySelector('#sf_textarea')?.checked) result.isTextarea = true;
      } else if (type === 'ENUM') {
        const enumOptions = this._collectEnumOptions(modal);
        if (enumOptions.length > 0) result.enumOptions = enumOptions;
      } else if (type === 'FILE') {
        if (modal.querySelector('#sf_multiple')?.checked) result.multiple = true;
      } else if (type === 'NUMBER') {
        const min = modal.querySelector('#sf_min')?.value;
        const max = modal.querySelector('#sf_max')?.value;
        if (min !== '' && min !== undefined) result.minValue = parseFloat(min);
        if (max !== '' && max !== undefined) result.maxValue = parseFloat(max);
      }

      if (isNew) {
        this._schemaData.push(result);
      } else {
        this._schemaData[idx] = result;
      }

      modal._shouldCommit = true;
      bsModal.hide();
    });

    modal.addEventListener('hidden.bs.modal', () => {
      const shouldCommit = modal._shouldCommit;
      modal.remove();
      if (shouldCommit) {
        this._commitSchema();
        this._renderStartNodeConfig();
      }
      this.nodeConfigModal.show();
    });
  }

  _attachEnumEvents(modal) {
    modal.querySelector('#sf_enum_add')?.addEventListener('click', () => {
      const container = modal.querySelector('#sf_enum_options');
      container.insertAdjacentHTML('beforeend', `
        <div class="input-group input-group-sm mb-1 sf-enum-row">
          <input type="text" class="form-control" placeholder="Valeur">
          <input type="text" class="form-control" placeholder="Label (optionnel)">
          <button type="button" class="btn btn-light text-danger sf-enum-remove"><i class="ti ti-x"></i></button>
        </div>`);
      this._attachEnumRemoveEvents(modal);
    });
    this._attachEnumRemoveEvents(modal);
  }

  _attachEnumRemoveEvents(modal) {
    modal.querySelectorAll('.sf-enum-remove').forEach(btn => {
      btn.onclick = () => btn.closest('.sf-enum-row').remove();
    });
  }

  _collectEnumOptions(modal) {
    const rows = modal.querySelectorAll('.sf-enum-row');
    const options = [];
    rows.forEach(row => {
      const inputs = row.querySelectorAll('input');
      const value = inputs[0]?.value?.trim();
      const label = inputs[1]?.value?.trim();
      if (value) {
        options.push({ value, label: label || value });
      }
    });
    return options;
  }

  /**
   * Syncs the local schema data to the designer's inputSchema.
   */
  _commitSchema() {
    const oldNames = new Set((this.designer.inputSchema || []).map(f => f.name));
    this.designer.inputSchema = [...this._schemaData];
    const newNames = new Set(this._schemaData.map(f => f.name));

    const removed = [...oldNames].filter(n => !newNames.has(n));
    if (removed.length > 0) {
      this._cleanOrphanedReferences(removed);
    }
    this._resolveFileAcceptTypes();
  }

  /**
   * Resolves accepted content types for FILE fields by inspecting consuming nodes.
   */
  _resolveFileAcceptTypes() {
    const fileFields = this.designer.inputSchema.filter(f => f.type === 'FILE');
    if (fileFields.length === 0) return;

    for (const field of fileFields) {
      const pattern = `{{sys.${field.name}}}`;
      const acceptSets = [];

      for (const node of Object.values(this.designer.nodesById)) {
        if (!node.data || node.type.toUpperCase() === 'START') continue;

        const referencesField = Object.values(node.data).some(v => typeof v === 'string' && v === pattern);
        if (!referencesField) continue;

        const nodeVars = (this.designer.nodeTypes[node.type] || {}).variables || {};
        for (const varDef of Object.values(nodeVars)) {
          if (varDef.type === 'FILE' && Array.isArray(varDef.acceptedContentTypes)) {
            acceptSets.push(new Set(varDef.acceptedContentTypes));
          }
        }
      }

      delete field.accept;
      if (acceptSets.length > 0) {
        const intersection = acceptSets.reduce((acc, s) => new Set([...acc].filter(t => s.has(t))));
        field.acceptedContentTypes = [...intersection];
      } else {
        delete field.acceptedContentTypes;
      }
    }
  }

  /**
   * Clears node config values that reference removed inputSchema fields.
   *
   * @param {string[]} removedNames - removed field names
   */
  _cleanOrphanedReferences(removedNames) {
    const patterns = new Set(removedNames.map(n => `{{sys.${n}}}`));
    Object.values(this.designer.nodesById).forEach(node => {
      if (!node.data || node.type.toUpperCase() === 'START') return;
      let changed = false;
      for (const [key, value] of Object.entries(node.data)) {
        if (typeof value === 'string' && patterns.has(value)) {
          node.data[key] = '';
          changed = true;
        }
      }
      if (changed) {
        const summaryEl = node.el?.querySelector('.pf-config-summary');
        if (summaryEl) {
          this.designer.nodeManager._updateConfigSummary(node, summaryEl);
        }
      }
    });
  }

  /**
   * Commits the schema, persists the flow, and refreshes the start node summary.
   */
  _saveStartNodeConfig() {
    this._commitSchema();
    this.designer._updateFlowInput();
    this._refreshStartNodeSummary();
  }

  _saveNodeConfig() {
    if (!this._currentEditingNode) return;

    const node = this._currentEditingNode;
    const formElements = this.cfgEl.querySelectorAll('input,select,textarea');
    const newData = { ...(node.data || {}) };

    const enumFields = new Map();
    const objectFields = new Map();
    const checkboxArrayFields = new Map();

    formElements.forEach(inp => {
      const { name, type, value, checked } = inp;

      if (type === 'checkbox' && name.endsWith('[]')) {
        const baseName = name.slice(0, -2);
        if (!checkboxArrayFields.has(baseName)) {
          checkboxArrayFields.set(baseName, []);
        }
        if (checked) {
          checkboxArrayFields.get(baseName).push(value);
        }
        return;
      }

      let match;
      if ((match = name.match(/(.+)__(.+?)(\[\])?$/))) {
        const base = match[1];
        const subfield = match[2].replace(/\[\]$/, '');

        if (!objectFields.has(base)) {
          objectFields.set(base, { fields: new Map() });
        }

        const objectField = objectFields.get(base);
        if (!objectField.fields.has(subfield)) {
          objectField.fields.set(subfield, []);
        }

        objectField.fields.get(subfield).push(value);
        return;
      }

      if (name.endsWith('_select') || name.endsWith('_input') || name.endsWith('_use_variable')) {
        const baseName = name.replace(/_select$|_input$|_use_variable$/, '');

        if (!enumFields.has(baseName)) {
          enumFields.set(baseName, { useVariable: false, selectValue: '', inputValue: '' });
        }

        const enumData = enumFields.get(baseName);
        if (name.endsWith('_select')) enumData.selectValue = value;
        if (name.endsWith('_input')) enumData.inputValue = value;
        if (name.endsWith('_use_variable')) enumData.useVariable = checked;

        return;
      }

      let fieldValue = value;
      if (type === 'checkbox') fieldValue = checked;
      else if (type === 'number') fieldValue = Number(value);

      newData[name] = fieldValue;
    });

    checkboxArrayFields.forEach((values, name) => {
      newData[name] = values;
    });

    enumFields.forEach((data, name) => {
      newData[name] = data.useVariable ? data.inputValue : data.selectValue;
    });

    objectFields.forEach((data, name) => {
      const def = this.designer.nodeTypes[node.type]?.variables?.[name];
      if (def && def.type === 'ARRAY') {
        const itemSchema = def.itemSchema || {};
        const schemaFields = (itemSchema.type === 'OBJECT' && itemSchema.fields) ? itemSchema.fields : [];

        let maxItems = 0;
        data.fields.forEach(values => {
          maxItems = Math.max(maxItems, values.length);
        });

        const items = [];
        for (let i = 0; i < maxItems; i++) {
          const item = {};
          let hasValue = false;

          schemaFields.forEach(field => {
            const fieldName = field.name;
            if (data.fields.has(fieldName)) {
              const values = data.fields.get(fieldName);
              if (values && values[i]) {
                item[fieldName] = values[i];
                hasValue = true;
              }
            }
          });

          if (hasValue) {
            items.push(item);
          }
        }

        newData[name] = items;
      } else {
        const obj = {};
        const keys = data.fields.get('key') || [];
        const values = data.fields.get('value') || [];

        keys.forEach((key, i) => {
          if (key && values[i] !== undefined) {
            obj[key] = values[i];
          }
        });

        newData[name] = obj;
      }
    });

    node.data = newData;

    this._scanForCustomVariables();
    if (this.tribute) {
      this.tribute.collection[0].values = this.availableOutputKeys;
    }

    const summaryEl = node.el.querySelector('.pf-config-summary');
    if (summaryEl) {
      this.designer.nodeManager._updateConfigSummary(node, summaryEl);
    }

    this.designer._updateFlowInput();
  }

  async _openPipelineEditModal() {
    if (!this.designer.pipelineId) {
      console.error("Cannot edit: No pipeline selected");
      return;
    }

    this.editFormContainer.innerHTML = `
            <div class="d-flex align-items-center justify-content-center py-5">
                <div class="spinner-border text-primary me-3" role="status">
                    <span class="visually-hidden">Chargement...</span>
                </div>
                <p class="text-muted mb-0">Chargement des informations du pipeline...</p>
            </div>`;

    this.pipelineEditModal.show();

    try {
      const response = await fetch(`rest/platform/agent/admin/pipelines/${this.designer.pipelineId}`);
      if (!response.ok) {
        throw new Error(`HTTP error: ${response.status}`);
      }

      const pipeline = await response.json();
      this._renderPipelineEditForm(pipeline);
    } catch (error) {
      this.editFormContainer.innerHTML = `
                <div class="alert alert-danger">
                    <div class="d-flex align-items-center">
                        <i class="ti ti-alert-circle fs-3 me-3"></i>
                        <div>
                            <h5 class="alert-heading mb-2">Erreur lors du chargement des données du pipeline</h5>
                            <p class="mb-0">${error.message}</p>
                        </div>
                    </div>
                </div>`;
    }
  }

  _renderPipelineEditForm(pipeline) {
    if (!pipeline) {
      this.editFormContainer.innerHTML = `
                <div class="alert alert-info">
                    <i class="ti ti-info-circle me-2"></i>
                    Aucune donnée de pipeline disponible.
                </div>`;
      return;
    }

    const html = `
            <form id="pipeline-edit-form" class="needs-validation" novalidate>
                <div class="mb-4">
                    <label for="pipeline-name" class="form-label fw-medium mb-0">
                        Nom <span class="text-danger">*</span>
                    </label>
                    <input type="text" class="form-control" id="pipeline-name" name="name" 
                           value="${pipeline.name || ''}" required maxlength="255">
                    <div class="invalid-feedback">Le nom du pipeline est requis.</div>
                </div>

                <div class="mb-4">
                    <label for="pipeline-description" class="form-label fw-medium mb-0">Description</label>
                    <textarea class="form-control" id="pipeline-description" name="description" 
                              rows="3">${pipeline.description || ''}</textarea>
                </div>

                <div class="mb-4">
                    <label for="pipeline-max-workers" class="form-label fw-medium mb-0">
                        Nombre maximum de travailleurs concurrents <span class="text-danger">*</span>
                    </label>
                    <input type="number" class="form-control" id="pipeline-max-workers" 
                           name="maxConcurrentWorkers" value="${pipeline.maxConcurrentWorkers || 1}" 
                           required min="1">
                    <div class="invalid-feedback">
                        Le nombre de travailleurs doit être un nombre positif.
                    </div>
                    <div class="form-text text-muted">
                        Détermine combien d'instances de ce pipeline peuvent s'exécuter simultanément.
                    </div>
                </div>
            </form>`;

    this.editFormContainer.innerHTML = html;

    const form = document.getElementById('pipeline-edit-form');
    form.addEventListener('submit', event => {
      if (!form.checkValidity()) {
        event.preventDefault();
        event.stopPropagation();
      }
      form.classList.add('was-validated');
    });
  }

  async _submitPipelineEditForm() {
    const form = document.getElementById('pipeline-edit-form');
    if (!form.checkValidity()) {
      form.classList.add('was-validated');
      return;
    }

    const formData = new FormData(form);
    const pipeline = {
      id: this.designer.pipelineId,
      name: formData.get('name'),
      description: formData.get('description'),
      maxConcurrentWorkers: parseInt(formData.get('maxConcurrentWorkers'), 10),
      flow: JSON.stringify(this.designer.getFlow())
    };

    try {
      const response = await fetch(`rest/platform/agent/admin/pipelines/${this.designer.pipelineId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(pipeline)
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || `HTTP error: ${response.status}`);
      }

      await response.json();

      this.editFormContainer.innerHTML = `
                <div class="text-success py-3">
                    Succès&nbsp;: le pipeline a été mis à jour.
                </div>`;

      setTimeout(() => {
        this.pipelineEditModal.hide();
      }, 1500);

    } catch (error) {
      const errorDiv = document.createElement('div');
      errorDiv.className = 'alert alert-danger mt-3';
      errorDiv.innerHTML = `
                <div class="d-flex align-items-center">
                    <div>Erreur lors de la mise à jour du pipeline : ${error.message}</div>
                </div>`;
      form.appendChild(errorDiv);
    }
  }

  async _openPipelineExecModal() {
    if (!this.designer.pipelineId) return;

    this.execFormContainer.innerHTML = `
            <div class="d-flex align-items-center justify-content-center py-5">
                <div class="spinner-border text-primary me-3" role="status">
                    <span class="visually-hidden">Chargement...</span>
                </div>
                <p class="text-muted mb-0">Chargement des paramètres requis...</p>
            </div>`;

    this.pipelineExecModal.show();

    try {
      this._renderPipelineExecForm(this.designer.inputSchema || []);

    } catch (e) {
      this.execFormContainer.innerHTML = `
                <div class="alert alert-danger">
                    <div class="d-flex align-items-center">
                        <i class="ti ti-alert-circle fs-3 me-3"></i>
                        <div>
                            <h5 class="alert-heading mb-2">Erreur</h5>
                            <p class="mb-0">${e.message}</p>
                        </div>
                    </div>
                </div>`;
    }
  }

  _renderPipelineExecForm(schema) {
    if (!schema || schema.length === 0) {
      this.execFormContainer.innerHTML = `
                <div class="alert alert-info">
                    <i class="ti ti-info-circle me-2"></i>
                    Aucun paramètre d'entrée requis.
                </div>`;
      return;
    }

    let html = '<form id="pipeline-exec-form">';
    for (const field of schema) {
      html += this._inputFieldHTML(field);
    }
    html += '</form>';

    this.execFormContainer.innerHTML = html;
  }

  _inputFieldHTML(def) {
    const id = `exec_input_${def.name}`;
    const label = def.title || def.name;
    const desc = def.description ? `<div class="form-text small text-muted mb-1">${def.description}</div>` : '';
    const req = def.required ? 'required' : '';

    if (def.type === 'FILE') {
      const multiple = def.multiple ? 'multiple' : '';
      const accept = Array.isArray(def.acceptedContentTypes) ? `accept="${def.acceptedContentTypes.join(',')}"` : '';
      return `
                <div class="mb-3">
                    <label for="${id}" class="form-label fw-medium mb-0">
                        ${label}${def.required ? ' <span class="text-danger">*</span>' : ''}
                    </label>
                    ${desc}
                    <input type="file" class="form-control" id="${id}" name="${def.name}"
                           ${multiple} ${accept} ${req} data-file-input="true" data-multiple="${!!def.multiple}">
                    ${def.required ? `<div class="invalid-feedback">Ce champ est requis</div>` : ''}
                </div>`;
    }

    const enumOpts = def.enumOptions || def.options;
    if (def.type === 'ENUM' && Array.isArray(enumOpts)) {
      const opts = enumOpts
        .map(o => `<option value="${o.value}">${o.label || o.value}</option>`)
        .join('');

      return `
                <div class="mb-3">
                    <label for="${id}" class="form-label fw-medium mb-0">
                        ${label}${def.required ? ' <span class="text-danger">*</span>' : ''}
                    </label>
                    ${desc}
                    <select class="form-select" id="${id}" name="${def.name}" ${req}>
                        <option value="" disabled selected>Sélectionnez une option...</option>
                        ${opts}
                    </select>
                    ${def.required ? `<div class="invalid-feedback">Ce champ est requis</div>` : ''}
                </div>`;
    }

    if (def.type === 'STRING' && def.isTextarea) {
      return `
                <div class="mb-3">
                    <label for="${id}" class="form-label fw-medium mb-0">
                        ${label}${def.required ? ' <span class="text-danger">*</span>' : ''}
                    </label>
                    ${desc}
                    <textarea class="form-control" id="${id}" name="${def.name}" rows="3" 
                              placeholder="${def.placeholder || ''}" ${req}></textarea>
                    ${def.required ? `<div class="invalid-feedback">This field is required</div>` : ''}
                </div>`;
    }

    if (def.type === 'STRING') {
      return `
                <div class="mb-3">
                    <label for="${id}" class="form-label fw-medium mb-0">
                        ${label}${def.required ? ' <span class="text-danger">*</span>' : ''}
                    </label>
                    ${desc}
                    <input type="text" class="form-control" id="${id}" name="${def.name}" 
                           placeholder="${def.placeholder || ''}" ${req}>
                    ${def.required ? `<div class="invalid-feedback">This field is required</div>` : ''}
                </div>`;
    }

    return `
            <div class="mb-3">
                <label for="${id}" class="form-label fw-medium mb-0">
                    ${label}${def.required ? ' <span class="text-danger">*</span>' : ''}
                </label>
                ${desc}
                <input type="text" class="form-control" id="${id}" name="${def.name}" 
                       placeholder="${def.placeholder || ''}" ${req}>
                ${def.required ? `<div class="invalid-feedback">This field is required</div>` : ''}
            </div>`;
  }

  async _submitPipelineExecForm() {
    const inputs = {};
    const form = document.getElementById('pipeline-exec-form');

    if (form) {
      if (!form.checkValidity()) {
        form.classList.add('was-validated');
        return;
      }

      const formData = new FormData(form);
      for (const [k, v] of formData.entries()) {
        if (v instanceof File && v.size === 0) continue;
        if (!(v instanceof File)) {
          inputs[k] = v;
        }
      }

      const fileInputs = form.querySelectorAll('input[data-file-input="true"]');
      for (const fileInput of fileInputs) {
        const files = fileInput.files;
        if (!files || files.length === 0) continue;
        const isMultiple = fileInput.dataset.multiple === 'true';
        const fileObjects = await this._filesToBase64(files);
        inputs[fileInput.name] = isMultiple ? fileObjects : fileObjects[0];
      }
    }

    this.pipelineExecModal.hide();

    this.designer.execStatusArea.innerHTML = `
            <div class="alert alert-info m-3">
                <div class="d-flex align-items-center">
                    <div class="spinner-border spinner-border-sm me-2" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                    <div>Démarrage de l'exécution...</div>
                </div>
            </div>`;

    this.designer.resetAllNodeStatus();
    this.designer.executionManager._startPipelineExecution(inputs);
  }

  /**
   * Converts FileList to array of base64 file objects.
   * @param {FileList} files the files to convert
   * @returns {Promise<Array>} array of {fileName, contentType, content} objects
   */
  _filesToBase64(files) {
    const promises = Array.from(files).map(file => {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
          const base64 = reader.result.split(',')[1];
          resolve({ fileName: file.name, contentType: file.type, content: base64 });
        };
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });
    });
    return Promise.all(promises);
  }

  initTribute() {
    if (typeof Tribute === 'undefined') {
      console.warn('Tribute.js is not loaded, variable autocompletion will not be available.');
      return;
    }

    this._scanForCustomVariables();
    this._updateAvailableOutputKeys();

    if (!this.availableOutputKeys || this.availableOutputKeys.length === 0) {
      this.availableOutputKeys = [];
    }

    this.tribute = new Tribute({
      containerClass: 'tribute-container dropdown-menu show',
      trigger: '{{',
      values: this.availableOutputKeys,
      lookup: 'key',
      fillAttr: 'key',
      menuShowMinLength: 0,
      noMatchTemplate: function (item) {
        const currentValue = this.current.element.value;
        const cursorPos = this.current.element.selectionStart;
        let prefix = '';

        for (let i = cursorPos - 1; i >= 0; i--) {
          if (currentValue.substring(i, i + 2) === '{{') {
            prefix = currentValue.substring(i + 2, cursorPos);
            break;
          }
        }

        return '<span class="dropdown-item text-secondary small">Aucune variable trouvée</span>';
      },
      menuItemTemplate: function (item) {
        return `<span class="dropdown-item d-flex justify-content-between align-items-center"><span class="node-key">${item.original.key}</span><span class="badge text-bg-secondary ms-2" style="font-size:.65em">${item.original.description}</span></span>`;
      },
      selectTemplate: function (item) {
        return `{{${item.original.key}}}`;
      }
    });

    this._attachTributeToFields();
    this._observeModalChanges();
  }

  _extractAndAddVariables(text, inputField) {
    if (typeof text !== 'string' || !inputField) return;

    const fieldId = inputField.id || inputField.name || Date.now() + Math.random();
    const currentFieldVars = new Set();

    const sysMatches = text.match(/\{\{sys\.[^}]+\}\}/g);
    if (sysMatches) {
      sysMatches.forEach(match => {
        const varName = match.substring(2, match.length - 2);
        currentFieldVars.add(varName);
      });
    }

    this.activeFieldVariables.set(fieldId, currentFieldVars);

    const allActiveVars = new Set();
    this.activeFieldVariables.forEach(vars => {
      vars.forEach(v => allActiveVars.add(v));
    });

    let hasChanges = false;
    allActiveVars.forEach(v => {
      if (!this.customVariables.has(v)) {
        this.customVariables.add(v);
        hasChanges = true;
        console.log('Variable ajoutée pendant la saisie:', v);
      }
    });

    if (hasChanges) {
      this._updateAvailableOutputKeys();
      if (this.tribute) {
        this.tribute.collection[0].values = this.availableOutputKeys;
      }
      this._refreshStartNodeSummary();
    }

    clearTimeout(this._scanTimeout);
    this._scanTimeout = setTimeout(() => {
      this._performFullVariableScan();
    }, 300);
  }

  _performFullVariableScan() {
    const configuredVars = new Set();

    Object.values(this.designer.nodesById).forEach(node => {
      Object.entries(node.data || {}).forEach(([key, value]) => {
        if (typeof value === 'string') {
          const matches = value.match(/\{\{sys\.[^}]+\}\}/g);
          if (matches) {
            matches.forEach(match => {
              const varName = match.substring(2, match.length - 2);
              configuredVars.add(varName);
            });
          }
        }
      });
    });

    const allActiveVars = new Set();
    this.activeFieldVariables.forEach(vars => {
      vars.forEach(v => allActiveVars.add(v));
    });

    const allVars = new Set([...configuredVars, ...allActiveVars]);

    let hasChanges = false;

    allVars.forEach(v => {
      if (!this.customVariables.has(v)) {
        this.customVariables.add(v);
        hasChanges = true;
      }
    });

    this.customVariables.forEach(v => {
      if (!allVars.has(v)) {
        this.customVariables.delete(v);
        hasChanges = true;
        console.log('Variable supprimée lors du scan complet:', v);
      }
    });

    if (hasChanges) {
      this._updateAvailableOutputKeys();
      if (this.tribute) {
        this.tribute.collection[0].values = this.availableOutputKeys;
      }
      this._refreshStartNodeSummary();
    }
  }

  /**
   * Refreshes the config summary of all Start nodes.
   */
  _refreshStartNodeSummary() {
    Object.values(this.designer.nodesById).forEach(n => {
      if (n.type.toUpperCase() === 'START') {
        const summaryEl = n.el.querySelector('.pf-config-summary');
        if (summaryEl) {
          this.designer.nodeManager._renderStartNodeSummary(summaryEl);
        }
      }
    });
  }

  _updateAvailableOutputKeys() {
    this.availableOutputKeys = [];

    const schema = this.designer.inputSchema || [];
    schema.forEach(field => {
      this.availableOutputKeys.push({
        key: `sys.${field.name}`,
        description: field.title || field.name
      });
    });

    let precedingNodeIds = [];
    if (this._currentEditingNode) {
      precedingNodeIds = this.designer.getPrecedingNodes(this._currentEditingNode.id);
    } else {
      precedingNodeIds = Object.keys(this.designer.nodesById);
    }

    precedingNodeIds.forEach(nodeId => {
      const node = this.designer.nodesById[nodeId];
      if (!node) return;

      const nodeTypeInfo = this.designer.nodeTypes[node.type] || {};
      const outputKeys = nodeTypeInfo.outputKeys || {};

      Object.entries(outputKeys).forEach(([key, description]) => {
        if (key === "__dynamic__") {
          this.availableOutputKeys.push({
            key: `${node.id}.*`,
            description: `${description} (${node.id})`
          });
        } else {
          this.availableOutputKeys.push({
            key: `${node.id}.${key}`,
            description: `${description} (${node.id})`
          });
        }
      });
    });
  }

  _scanForCustomVariables() {
    this._performFullVariableScan();
  }

  _attachTributeToFields() {
    if (!this.tribute) return;

    const textInputs = document.querySelectorAll('input[type="text"], textarea');
    textInputs.forEach(input => {
      if (input.closest('#modal-node-config, #pipeline-exec-form-container')) {
        if (!input.dataset.tributeAttached) {
          this.tribute.attach(input);
          input.dataset.tributeAttached = 'true';
          input.classList.add('tribute-enabled');

          const fieldId = input.id || input.name || Date.now() + Math.random();
          input.dataset.fieldId = fieldId;

          if (this._currentEditingNode) {
            input.dataset.nodeId = this._currentEditingNode.id;
          }

          input.addEventListener('input', (e) => {
            this._extractAndAddVariables(e.target.value, e.target);
          });

          this._extractAndAddVariables(input.value, input);

          const modal = input.closest('.modal');
          if (modal && !modal.dataset.tributeCleanupAttached) {
            modal.dataset.tributeCleanupAttached = 'true';
            modal.addEventListener('hidden.bs.modal', () => {
              this.activeFieldVariables.clear();
              this._performFullVariableScan();
            });
          }
        }
      }
    });
  }

  _initPipelineExecStatusModal() {
    let execStatusModalEl = document.getElementById('pipelineExecStatusModal');
    if (!execStatusModalEl) {
      execStatusModalEl = document.createElement('div');
      execStatusModalEl.className = 'modal fade';
      execStatusModalEl.id = 'pipelineExecStatusModal';
      execStatusModalEl.tabIndex = -1;
      execStatusModalEl.setAttribute('aria-labelledby', 'pipelineExecStatusModalLabel');
      execStatusModalEl.setAttribute('aria-hidden', 'true');
      execStatusModalEl.innerHTML = `
                <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl">
                    <div class="modal-content">
                        <div class="modal-header">
                            <div class="d-flex align-items-center">
                                <i class="ti ti-activity me-2 text-primary"></i>
                                <h5 class="modal-title fw-bold" id="pipelineExecStatusModalLabel">Exécution du pipeline</h5>
                            </div>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
                        </div>
                        <div class="modal-body">
                            <div id="exec-status-area">
                                <div class="d-flex flex-column align-items-center justify-content-center text-center p-4" style="min-height: 300px;">
                                    <div class="mb-3">
                                        <i class="ti ti-player-play fs-1 text-muted opacity-25"></i>
                                    </div>
                                    <p class="text-muted">Aucune exécution active.</p>
                                    <p class="small text-muted">Cliquez sur le bouton play pour exécuter ce pipeline.</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>`;
      document.body.appendChild(execStatusModalEl);
    }

    this.execStatusModalEl = execStatusModalEl;
    this.pipelineExecStatusModal = new bootstrap.Modal(this.execStatusModalEl);
    this.designer.execStatusArea = document.getElementById('exec-status-area');
  }

  _observeModalChanges() {
    if (!this.tribute) return;

    const config = { childList: true, subtree: true };

    const observer = new MutationObserver(mutations => {
      let shouldAttach = false;
      mutations.forEach(mutation => {
        if (mutation.addedNodes.length) {
          shouldAttach = true;
        }
      });

      if (shouldAttach) {
        setTimeout(() => this._attachTributeToFields(), 50);
      }
    });

    if (this.cfgEl) {
      observer.observe(this.cfgEl, config);
    }

    if (this.execFormContainer) {
      observer.observe(this.execFormContainer, config);
    }
  }
}