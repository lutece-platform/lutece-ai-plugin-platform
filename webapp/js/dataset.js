class DatasetManager {
    constructor(datasetId, options = {}) {
        this.datasetId = datasetId;
        this.options = {
            refreshInterval: 3000,
            i18nTexts: {
                pending: 'En attente',
                processing: 'En cours',
                completed: 'Terminé',
                error: 'Erreur'
            },
            ...options
        };
        this.intervalId = null;
    }

    formatStatus(status) {
        let badgeColor = 'secondary';
        let icon = 'clock';
        let statusText = status;

        switch (status) {
            case 'pending':
                badgeColor = 'warning';
                icon = 'clock';
                statusText = this.options.i18nTexts.pending;
                break;
            case 'processing':
                badgeColor = 'primary';
                icon = 'loader ti-spin';
                statusText = this.options.i18nTexts.processing;
                break;
            case 'completed':
                badgeColor = 'success';
                icon = 'check';
                statusText = this.options.i18nTexts.completed;
                break;
            case 'error':
                badgeColor = 'danger';
                icon = 'alert-triangle';
                statusText = this.options.i18nTexts.error;
                break;
        }

        return {
            color: badgeColor,
            icon: icon,
            text: statusText
        };
    }

    async fetchJobStatuses() {
        try {
            const response = await fetch(`rest/platform/agent/dataset/jobs/${this.datasetId}`);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }

            const data = await response.json();
            const jobs = data.result || [];

            jobs.forEach(job => {
                const status = this.formatStatus(job.status);
                const statusBadge = document.getElementById(`doc-status-${job.documentId}`);
                const updatedCell = document.getElementById(`doc-updated-${job.documentId}`);

                if (statusBadge) {
                    statusBadge.className = `badge text-bg-${status.color}`;
                    statusBadge.textContent = status.text;

                    if (job.status === 'error' && job.errorMessage) {
                        statusBadge.setAttribute('title', job.errorMessage);
                        if (typeof bootstrap !== 'undefined') {
                            new bootstrap.Tooltip(statusBadge);
                        }
                    } else {
                        statusBadge.removeAttribute('title');
                        if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip.getInstance(statusBadge)) {
                            bootstrap.Tooltip.getInstance(statusBadge).dispose();
                        }
                    }
                }

                if (updatedCell && job.updated) {
                    updatedCell.textContent = new Date(job.updated).toLocaleString();
                }
            });
        } catch (error) {
            console.error('Error fetching jobs:', error);
        }
    }

    startMonitoring() {
        this.fetchJobStatuses();
        this.intervalId = setInterval(() => this.fetchJobStatuses(), this.options.refreshInterval);
    }

    stopMonitoring() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
            this.intervalId = null;
        }
    }

    initEventListeners() {
        const refreshButton = document.getElementById('refresh-jobs-btn');
        if (refreshButton) {
            refreshButton.addEventListener('click', () => this.fetchJobStatuses());
        }

        window.addEventListener('beforeunload', () => this.stopMonitoring());
    }

    init() {
        this.initEventListeners();
        this.startMonitoring();
    }
}

export { DatasetManager };