export default class EventManager {
  constructor(pipelineDesigner) {
    this.designer = pipelineDesigner;
    this._isPanning = false;
    this._lastMouseX = 0;
    this._lastMouseY = 0;
    this._initEventHandlers();
  }
  _initEventHandlers() {
    this.designer.container.addEventListener('mousedown', e => {
      if (e.button === 0 && (e.target === this.designer.container || e.target === this.designer.svg)) {
        this._isPanning = true;
        this._lastMouseX = e.clientX;
        this._lastMouseY = e.clientY;
        this.designer.container.style.cursor = 'grabbing';
        e.preventDefault();
      }
    });
    this.designer.container.addEventListener('mousemove', e => {
      if (!this._isPanning) return;
      const dx = e.clientX - this._lastMouseX;
      const dy = e.clientY - this._lastMouseY;
      this.designer.renderManager._panX += dx;
      this.designer.renderManager._panY += dy;
      this._lastMouseX = e.clientX;
      this._lastMouseY = e.clientY;
      this.designer.renderManager._updateTransform();
    });
    window.addEventListener('mouseup', () => {
      if (this._isPanning) {
        this._isPanning = false;
        this.designer.container.style.cursor = 'default';
      }
    });
    this.designer.container.addEventListener('keydown', e => {
      if ((e.key === 'Delete' || e.key === 'Backspace') && this.designer._selected) {
        this.designer.removeNode(this.designer._selected);
      }
    });
    this.designer.container.addEventListener('dragover', e => e.preventDefault());
    this.designer.container.addEventListener('drop', e => {
      e.preventDefault();
      const type = e.dataTransfer.getData('nodeType');
      if (!type) return;
      const { left, top } = this.designer.container.getBoundingClientRect();
      const x = e.clientX - left + this.designer.container.scrollLeft;
      const y = e.clientY - top + this.designer.container.scrollTop;
      this.designer.addNode(type, { x, y });
    });
    this.designer.container.addEventListener('contextmenu', e => {
      if (e.target.closest('.pf-port') || e.target.closest('.pf-node')) return;
      e.preventDefault();
      const { left, top } = this.designer.container.getBoundingClientRect();
      const x = e.clientX - left + this.designer.container.scrollLeft;
      const y = e.clientY - top + this.designer.container.scrollTop;
      this.designer.uiManager._showNodeTypeMenu(x, y, e.clientX, e.clientY);
    });
    this.designer.container.addEventListener('wheel', e => {
      e.preventDefault();
      const rect = this.designer.container.getBoundingClientRect();
      const mouseX = e.clientX - rect.left;
      const mouseY = e.clientY - rect.top;
      const preZoomX = (mouseX - this.designer.renderManager._panX) / this.designer.renderManager._zoomLevel;
      const preZoomY = (mouseY - this.designer.renderManager._panY) / this.designer.renderManager._zoomLevel;
      const factor = 1.2;
      if (e.deltaY < 0) {
        this.designer.renderManager._zoomLevel = Math.min(this.designer.renderManager._zoomLevel * factor, 5);
      } else {
        this.designer.renderManager._zoomLevel = Math.max(this.designer.renderManager._zoomLevel / factor, 0.2);
      }
      this.designer.renderManager._panX = mouseX - preZoomX * this.designer.renderManager._zoomLevel;
      this.designer.renderManager._panY = mouseY - preZoomY * this.designer.renderManager._zoomLevel;
      this.designer.renderManager._updateTransform();
    });
  }
}