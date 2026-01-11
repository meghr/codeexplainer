/**
 * Code Explainer Web Application
 */
class CodeExplainerApp {
    constructor() {
        this.sessionId = null;
        this.initElements();
        this.initEventListeners();
    }

    initElements() {
        // Views
        this.views = {
            upload: document.getElementById('uploadView'),
            classes: document.getElementById('classesView'),
            endpoints: document.getElementById('endpointsView'),
            components: document.getElementById('componentsView'),
            ioAnalysis: document.getElementById('ioAnalysisView'),
            diagrams: document.getElementById('diagramsView'),
            docs: document.getElementById('docsView')
        };

        // Navigation
        this.navItems = document.querySelectorAll('.nav-item');

        // Upload elements
        this.uploadZone = document.getElementById('uploadZone');
        this.fileInput = document.getElementById('fileInput');
        this.uploadProgress = document.getElementById('uploadProgress');
        this.progressFill = document.getElementById('progressFill');
        this.statusText = document.getElementById('statusText');
        this.analysisSummary = document.getElementById('analysisSummary');

        // Session info
        this.sessionInfo = document.getElementById('sessionInfo');
        this.sessionIdEl = document.getElementById('sessionId');
    }

    initEventListeners() {
        // Navigation
        this.navItems.forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const view = item.dataset.view;
                this.showView(view);
            });
        });

        // Upload zone
        this.uploadZone.addEventListener('click', () => this.fileInput.click());
        this.fileInput.addEventListener('change', (e) => this.handleFileSelect(e));

        // Drag and drop
        this.uploadZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            this.uploadZone.classList.add('drag-over');
        });
        this.uploadZone.addEventListener('dragleave', () => {
            this.uploadZone.classList.remove('drag-over');
        });
        this.uploadZone.addEventListener('drop', (e) => {
            e.preventDefault();
            this.uploadZone.classList.remove('drag-over');
            if (e.dataTransfer.files.length > 0) {
                this.uploadFile(e.dataTransfer.files[0]);
            }
        });

        // Class search
        const classSearch = document.getElementById('classSearch');
        if (classSearch) {
            classSearch.addEventListener('input', (e) => this.filterClasses(e.target.value));
        }

        // Export buttons
        const exportJsonBtn = document.getElementById('exportJson');
        if (exportJsonBtn) {
            exportJsonBtn.addEventListener('click', () => this.exportReport('json'));
        }

        const exportPdfBtn = document.getElementById('exportPdf');
        if (exportPdfBtn) {
            exportPdfBtn.addEventListener('click', () => this.exportReport('pdf'));
        }

        // Diagram download buttons
        const downloadSvgBtn = document.getElementById('downloadSvg');
        if (downloadSvgBtn) {
            downloadSvgBtn.addEventListener('click', () => this.downloadDiagram('svg'));
        }

        const downloadPngBtn = document.getElementById('downloadPng');
        if (downloadPngBtn) {
            downloadPngBtn.addEventListener('click', () => this.downloadDiagram('png'));
        }
    }

    showView(viewName) {
        // Check if we have a session for views that require it
        if (viewName !== 'upload' && !this.sessionId) {
            this.showNotification('Please upload a JAR file first', 'warning');
            return;
        }

        // Update nav active state
        this.navItems.forEach(item => {
            item.classList.toggle('active', item.dataset.view === viewName);
        });

        // Show the correct view
        Object.keys(this.views).forEach(key => {
            const view = this.views[key];
            if (view) {
                view.classList.toggle('active', key === viewName);
            }
        });

        // Load data for the view
        switch (viewName) {
            case 'classes':
                this.loadClasses();
                break;
            case 'endpoints':
                this.loadEndpoints();
                break;
            case 'components':
                this.loadComponents();
                break;
            case 'ioAnalysis':
                this.loadIOAnalysis();
                break;
            case 'docs':
                this.loadDocumentation();
                break;
            case 'diagrams':
                this.loadDiagrams();
                break;
        }
    }

    handleFileSelect(event) {
        const file = event.target.files[0];
        if (file) {
            this.uploadFile(file);
        }
    }

    async uploadFile(file) {
        if (!file.name.endsWith('.jar')) {
            this.showNotification('Please select a JAR file', 'error');
            return;
        }

        // Show progress
        this.uploadZone.style.display = 'none';
        this.uploadProgress.style.display = 'block';
        this.analysisSummary.style.display = 'none';

        const formData = new FormData();
        formData.append('file', file);

        try {
            // Animate progress
            let progress = 0;
            const progressInterval = setInterval(() => {
                progress += Math.random() * 20;
                if (progress > 90) progress = 90;
                this.progressFill.style.width = progress + '%';
            }, 200);

            const response = await fetch('/api/analyze', {
                method: 'POST',
                body: formData
            });

            clearInterval(progressInterval);
            this.progressFill.style.width = '100%';

            if (response.ok) {
                const data = await response.json();
                this.sessionId = data.sessionId;

                // Update session info
                this.sessionInfo.style.display = 'block';
                this.sessionIdEl.textContent = this.sessionId.substring(0, 8) + '...';

                // Show summary
                this.uploadProgress.style.display = 'none';
                this.analysisSummary.style.display = 'block';

                document.getElementById('classCount').textContent = data.classCount || 0;
                document.getElementById('methodCount').textContent = data.methodMetrics?.totalMethods || 0;
                document.getElementById('interfaceCount').textContent = data.classMetrics?.interfaceCount || 0;
                document.getElementById('packageCount').textContent = data.classMetrics?.packageCount || 0;

                this.showNotification('Analysis complete!', 'success');
            } else {
                throw new Error('Analysis failed');
            }
        } catch (error) {
            console.error('Upload error:', error);
            this.uploadProgress.style.display = 'none';
            this.uploadZone.style.display = 'block';
            this.showNotification('Error analyzing JAR file', 'error');
        }
    }

    async loadClasses() {
        try {
            const response = await fetch(`/api/sessions/${this.sessionId}/classes`);
            if (response.ok) {
                const classes = await response.json();
                this.renderClasses(classes);
            }
        } catch (error) {
            console.error('Error loading classes:', error);
        }
    }

    renderClasses(classes) {
        const tbody = document.getElementById('classesTableBody');
        tbody.innerHTML = classes.map(c => `
            <tr>
                <td>${c.className}</td>
                <td><code>${c.packageName}</code></td>
                <td>${c.type}</td>
                <td>${c.methodCount}</td>
                <td>${c.fieldCount}</td>
            </tr>
        `).join('');
    }

    filterClasses(query) {
        const rows = document.querySelectorAll('#classesTableBody tr');
        query = query.toLowerCase();
        rows.forEach(row => {
            const text = row.textContent.toLowerCase();
            row.style.display = text.includes(query) ? '' : 'none';
        });
    }

    async loadEndpoints() {
        try {
            const response = await fetch(`/api/sessions/${this.sessionId}/endpoints`);
            if (response.ok) {
                const endpoints = await response.json();
                this.renderEndpoints(endpoints);
            }
        } catch (error) {
            console.error('Error loading endpoints:', error);
        }
    }

    renderEndpoints(endpoints) {
        const container = document.getElementById('endpointsList');
        if (endpoints.length === 0) {
            container.innerHTML = '<p class="empty-message">No REST endpoints detected</p>';
            return;
        }
        container.innerHTML = endpoints.map(e => `
            <div class="endpoint-card">
                <span class="method-badge method-${(e.httpMethod || 'get').toLowerCase()}">${e.httpMethod || 'GET'}</span>
                <span class="endpoint-path">${e.urlPattern || e.path || '/'}</span>
                <span class="endpoint-handler">${e.handlerMethod || e.methodName || ''}</span>
            </div>
        `).join('');
    }

    async loadComponents() {
        try {
            const response = await fetch(`/api/sessions/${this.sessionId}/components`);
            if (response.ok) {
                const components = await response.json();
                this.renderComponents(components);
            }
        } catch (error) {
            console.error('Error loading components:', error);
        }
    }

    renderComponents(components) {
        const container = document.getElementById('componentsList');
        if (components.length === 0) {
            container.innerHTML = '<p class="empty-message">No Spring components detected</p>';
            return;
        }
        container.innerHTML = components.map(c => `
            <div class="component-card">
                <span class="component-type type-${(c.type || 'service').toLowerCase()}">${c.type || 'Service'}</span>
                <div class="component-name">${c.className}</div>
                <div class="component-package">${c.packageName}</div>
            </div>
        `).join('');
    }

    async loadIOAnalysis() {
        try {
            const response = await fetch(`/api/sessions/${this.sessionId}/io-analysis`);
            if (response.ok) {
                const data = await response.json();
                this.renderIOAnalysis(data);
            }
        } catch (error) {
            console.error('Error loading I/O analysis:', error);
        }
    }

    renderIOAnalysis(data) {
        const container = document.getElementById('ioContent');

        const dtos = data.dtos || [];
        const pairs = data.requestResponsePairs || [];

        container.innerHTML = `
            <div class="stats-grid">
                <div class="stat-card">
                    <span class="stat-value">${data.methodIOs?.length || 0}</span>
                    <span class="stat-label">Methods Analyzed</span>
                </div>
                <div class="stat-card">
                    <span class="stat-value">${dtos.length}</span>
                    <span class="stat-label">DTOs Detected</span>
                </div>
                <div class="stat-card">
                    <span class="stat-value">${pairs.length}</span>
                    <span class="stat-label">API Pairs</span>
                </div>
            </div>
            
            ${dtos.length > 0 ? `
                <h3 style="margin: 30px 0 15px;">Data Transfer Objects</h3>
                <div class="components-grid">
                    ${dtos.map(d => `
                        <div class="component-card">
                            <span class="component-type type-${d.type?.toLowerCase() || 'data'}">${d.type || 'DTO'}</span>
                            <div class="component-name">${d.className}</div>
                        </div>
                    `).join('')}
                </div>
            ` : ''}
        `;
    }

    async loadDocumentation() {
        try {
            const response = await fetch(`/api/sessions/${this.sessionId}/docs`);
            if (response.ok) {
                const docs = await response.json();
                this.renderDocumentation(docs);
            }
        } catch (error) {
            console.error('Error loading documentation:', error);
        }
    }

    renderDocumentation(docs) {
        const container = document.getElementById('docsContent');
        // Simple markdown-like rendering
        let html = docs.markdown || '';
        html = html.replace(/^### (.*$)/gm, '<h3>$1</h3>');
        html = html.replace(/^## (.*$)/gm, '<h2>$1</h2>');
        html = html.replace(/^# (.*$)/gm, '<h1>$1</h1>');
        html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
        html = html.replace(/\n/g, '<br>');
        container.innerHTML = html;
    }

    async loadDiagrams() {
        try {
            const response = await fetch(`/api/sessions/${this.sessionId}/diagram`);
            if (response.ok) {
                const plantUml = await response.text();
                this.renderDiagram(plantUml);
            }
        } catch (error) {
            console.error('Error loading diagram:', error);
        }
    }

    renderDiagram(plantUml) {
        const sourceContainer = document.getElementById('plantUmlSource');
        const img = document.getElementById('diagramImage');
        const loading = document.getElementById('diagramLoading');
        const errorMsg = document.getElementById('diagramError');

        // Show source
        sourceContainer.textContent = plantUml;

        // Reset state
        loading.style.display = 'block';
        loading.textContent = 'Rendering diagram...';
        img.style.display = 'none';
        errorMsg.style.display = 'none';

        try {
            // Use the global plantumlEncoder if available
            if (typeof plantumlEncoder !== 'undefined') {
                const encoded = plantumlEncoder.encode(plantUml);
                img.src = `http://www.plantuml.com/plantuml/svg/${encoded}`;
            } else {
                throw new Error('PlantUML Encoder library not loaded');
            }
        } catch (e) {
            console.error('Encoding error:', e);
            loading.style.display = 'none';
            errorMsg.textContent = 'Error encoding diagram: ' + e.message;
            errorMsg.style.display = 'block';
            return;
        }

        img.onload = () => {
            loading.style.display = 'none';
            img.style.display = 'inline-block';
        };

        img.onerror = () => {
            loading.style.display = 'none';
            errorMsg.textContent = 'Failed to render diagram. The diagram might be too complex or the PlantUML server is unreachable.';
            errorMsg.style.display = 'block';
        };
    }

    downloadDiagram(format) {
        const sourceContainer = document.getElementById('plantUmlSource');
        const plantUml = sourceContainer.textContent;

        if (!plantUml) {
            this.showNotification('No diagram to download', 'warning');
            return;
        }

        try {
            if (typeof plantumlEncoder !== 'undefined') {
                const encoded = plantumlEncoder.encode(plantUml);
                const url = `http://www.plantuml.com/plantuml/${format}/${encoded}`;

                // Open in new tab which triggers download/view (best for cross-origin)
                window.open(url, '_blank');
            } else {
                this.showNotification('Encoder library not ready', 'error');
            }
        } catch (e) {
            console.error('Error preparing download:', e);
            this.showNotification('Error preparing download', 'error');
        }
    }

    exportReport(format) {
        if (!this.sessionId) return;
        window.location.href = `/api/sessions/${this.sessionId}/export?format=${format}`;
    }

    showNotification(message, type = 'info') {
        console.log(`[${type.toUpperCase()}] ${message}`);
        // Could add a toast notification system here
    }
}

// Initialize the app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.app = new CodeExplainerApp();
});
