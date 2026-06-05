export default class RenderManager {
  constructor(pipelineDesigner) {
    this.designer = pipelineDesigner;
    
    this._zoomLevel = 1;
    this._panX = 0;
    this._panY = 0;
    
    this._initContainer();
  }
  
  _initContainer() {
    this.designer.container.classList.add('pf-designer');
    this.designer.container.tabIndex = 0;
    
    this.designer.svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    this.designer.svg.setAttribute('class', 'pf-connections');
    this.designer.container.appendChild(this.designer.svg);
    
    this.designer.nodeLayer = document.createElement('div');
    this.designer.nodeLayer.className = 'pf-nodes';
    this.designer.container.appendChild(this.designer.nodeLayer);
    
    this.designer.svg.style.transformOrigin = '0 0';
    this.designer.nodeLayer.style.transformOrigin = '0 0';
    
    this._createActionBar();
  }
  
_createActionBar() {
    this.actionBar = document.createElement('div');
    this.actionBar.className = 'pf-action-bar';
    this.actionBar.innerHTML = `
        <button id="pf-zoom-in" title="Agrandir" class="pf-action-btn">
            <i class="ti ti-plus"></i>
        </button>
        <button id="pf-zoom-out" title="Rétrécir" class="pf-action-btn">
            <i class="ti ti-minus"></i>
        </button>
        <button id="pf-center" title="Centrer la vue" class="pf-action-btn">
            <i class="ti ti-focus-centered"></i>
        </button>
        <button id="pf-edit" title="Modifier le pipeline" class="pf-action-btn">
            <i class="ti ti-edit"></i>
        </button>
        <button id="pf-play" title="Exécuter le pipeline" class="pf-action-btn">
            <i class="ti ti-player-play"></i>
        </button>`;

    this.designer.container.appendChild(this.actionBar);

    this.actionBar.querySelector('#pf-zoom-in').addEventListener('click', () => {
        this._zoomLevel = Math.min(this._zoomLevel * 1.2, 5);
        this._updateTransform();
    });

    this.actionBar.querySelector('#pf-zoom-out').addEventListener('click', () => {
        this._zoomLevel = Math.max(this._zoomLevel / 1.2, 0.2);
        this._updateTransform();
    });

    this.actionBar.querySelector('#pf-center').addEventListener('click', () => {
        this.centerView();
    });

    this.actionBar.querySelector('#pf-edit').addEventListener('click', () => {
        this.designer.uiManager._openPipelineEditModal();
    });

    this.actionBar.querySelector('#pf-play').addEventListener('click', () => {
        this.designer.uiManager._openPipelineExecModal();
    });

    this._addAutoFollowButton();
}
  
  _addAutoFollowButton() {
    const followBtn = document.createElement('button');
    followBtn.id = 'pf-auto-follow';
    followBtn.className = 'pf-action-btn';
    followBtn.title = 'Suivi automatique de l\'exécution';
    followBtn.innerHTML = '<i class="ti ti-focus"></i>';
    
    followBtn.classList.toggle('active', this.designer._autoFollow);
    
    followBtn.addEventListener('click', () => {
      this.designer._autoFollow = !this.designer._autoFollow;
      followBtn.classList.toggle('active', this.designer._autoFollow);
    });
    
    this.actionBar.appendChild(followBtn);
  }
  
  _updateTransform() {
    const t = `translate(${this._panX}px, ${this._panY}px) scale(${this._zoomLevel})`;
    this.designer.svg.style.transform = t;
    this.designer.nodeLayer.style.transform = t;
  }
  
  _getCoordsInCanvas(screenX, screenY) {
    return {
      x: (screenX - this._panX) / this._zoomLevel,
      y: (screenY - this._panY) / this._zoomLevel
    };
  }
  
  _getCoordsOnScreen(canvasX, canvasY) {
    return {
      x: canvasX * this._zoomLevel + this._panX,
      y: canvasY * this._zoomLevel + this._panY
    };
  }
  
  centerView() {
    const ids = Object.keys(this.designer.nodesById);
    
    if (ids.length === 0) {
      this._zoomLevel = 1;
      this._panX = 0;
      this._panY = 0;
      this._updateTransform();
      return;
    }
    
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    const nodeBuffer = 50;
    
    ids.forEach(id => {
      const node = this.designer.nodesById[id];
      const nodeWidth = node.el.offsetWidth || 220;
      const nodeHeight = node.el.offsetHeight || 120;
      
      minX = Math.min(minX, node.x - nodeBuffer);
      minY = Math.min(minY, node.y - nodeBuffer);
      maxX = Math.max(maxX, node.x + nodeWidth + nodeBuffer);
      maxY = Math.max(maxY, node.y + nodeHeight + nodeBuffer);
    });
    
    this.designer.edges.forEach(edge => {
      const p1 = this.designer.edgeManager._portCenter(edge.source, edge.sourcePort, true);
      const p2 = this.designer.edgeManager._portCenter(edge.target, edge.targetPort, false);
      
      const c1 = this._getCoordsInCanvas(p1.x, p1.y);
      const c2 = this._getCoordsInCanvas(p2.x, p2.y);
      
      minX = Math.min(minX, c1.x, c2.x);
      minY = Math.min(minY, c1.y, c2.y);
      maxX = Math.max(maxX, c1.x, c2.x);
      maxY = Math.max(maxY, c1.y, c2.y);
    });
    
    const centerX = (minX + maxX) / 2;
    const centerY = (minY + maxY) / 2;
    const width = maxX - minX;
    const height = maxY - minY;
    
    const pad = 40;
    const scaleX = (this.designer.container.clientWidth - pad * 2) / width;
    const scaleY = (this.designer.container.clientHeight - pad * 2) / height;
    
    const maxNodeWidth = 160;
    const baseNodeWidth = 140;
    const maxZoom = maxNodeWidth / baseNodeWidth;
    
    const zoom = Math.min(scaleX, scaleY, maxZoom);
    
    this._zoomLevel = Math.max(Math.min(zoom, maxZoom), 0.2);
    this._panX = (this.designer.container.clientWidth / 2) - centerX * this._zoomLevel;
    this._panY = (this.designer.container.clientHeight / 2) - centerY * this._zoomLevel;
    
    this._updateTransform();
  }
}