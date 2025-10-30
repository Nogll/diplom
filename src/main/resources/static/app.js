let currentPage = 0;
let currentInteractionsPage = 0;
let pageSize = 10;

// Global filter state for pagination
let currentFilters = {
    plantName: '',
    compoundName: '',
    effect: ''
};

document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('articleForm').addEventListener('submit', handleArticleSubmit);
    showUploadForm();
});

function showUploadForm() {
    hideAllSections();
    document.getElementById('uploadForm').style.display = 'block';
}

function showArticles() {
    hideAllSections();
    document.getElementById('articlesList').style.display = 'block';
    currentPage = 0;
    loadArticles();
}

function showInteractions() {
    hideAllSections();
    document.getElementById('interactionsList').style.display = 'block';
    currentInteractionsPage = 0;
    // Reset filters
    currentFilters = { plantName: '', compoundName: '', effect: '' };
    loadInteractions();
}

function hideAllSections() {
    document.getElementById('uploadForm').style.display = 'none';
    document.getElementById('articlesList').style.display = 'none';
    document.getElementById('interactionsList').style.display = 'none';
}

async function handleArticleSubmit(e) {
    e.preventDefault();
    
    const url = document.getElementById('articleUrl').value;
    const title = document.getElementById('articleTitle').value;
    const abstract = document.getElementById('articleAbstract').value;
    
    const resultDiv = document.getElementById('uploadResult');
    resultDiv.innerHTML = '<div class="spinner-border text-primary" role="status"></div>';
    
    try {
        const response = await fetch('/api/v1/articles/process', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ url, title, abstract })
        });
        
        const data = await response.json();
        
        if (data.success) {
            resultDiv.innerHTML = `
                <div class="alert alert-success">
                    <strong>Success!</strong> Article processed successfully. 
                    Found ${data.interactionsCount} interaction(s).
                </div>
            `;
            document.getElementById('articleForm').reset();
        } else {
            resultDiv.innerHTML = `
                <div class="alert alert-danger">
                    <strong>Error!</strong> ${data.message}
                </div>
            `;
        }
    } catch (error) {
        resultDiv.innerHTML = `
            <div class="alert alert-danger">
                <strong>Error!</strong> ${error.message}
            </div>
        `;
    }
}

async function loadArticles(page = 0) {
    const contentDiv = document.getElementById('articlesContent');
    contentDiv.innerHTML = '<div class="spinner-border text-primary" role="status"></div>';
    
    try {
        const response = await fetch(`/api/v1/articles?page=${page}&size=${pageSize}`);
        const data = await response.json();
        
        if (data.content.length === 0) {
            contentDiv.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📄</div>
                    <h3>No articles found</h3>
                    <p>Upload an article to get started</p>
                </div>
            `;
            return;
        }
        
        let html = '';
        data.content.forEach(article => {
            html += `
                <div class="article-card">
                    <h4>${escapeHtml(article.title)}</h4>
                    <p class="text-muted">${escapeHtml(article.url)}</p>
                    ${article.abstract ? `<p>${escapeHtml(article.abstract.substring(0, 200))}...</p>` : ''}
                    <button class="btn btn-sm btn-primary mt-2" onclick="loadArticleSources(${article.id}, event)">View Sources</button>
                    <div id="sources-${article.id}" class="mt-3"></div>
                </div>
            `;
        });
        
        contentDiv.innerHTML = html;
        renderPagination('articlesPagination', data.totalPages, data.currentPage, data.totalElements, loadArticles);
        
    } catch (error) {
        contentDiv.innerHTML = `
            <div class="alert alert-danger">
                <strong>Error!</strong> ${error.message}
            </div>
        `;
    }
}

async function loadInteractions(page = 0, plantName = '', compoundName = '', effect = '') {
    const contentDiv = document.getElementById('interactionsContent');
    contentDiv.innerHTML = '<div class="loading-state"><div class="spinner-border text-primary" role="status"></div></div>';
    
    try {
        const params = new URLSearchParams({
            page: page,
            size: pageSize
        });
        
        if (plantName) params.append('plantName', plantName);
        if (compoundName) params.append('compoundName', compoundName);
        if (effect) params.append('effect', effect);
        
        const response = await fetch(`/api/v1/interactions?${params}`);
        const data = await response.json();
        
        if (data.content.length === 0) {
            contentDiv.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🔬</div>
                    <h3>No interactions found</h3>
                    <p>Try adjusting your filters or upload an article</p>
                </div>
            `;
            return;
        }
        
        let html = '';
        data.content.forEach(interaction => {
            const effectsHtml = interaction.effects.map(e => 
                `<span class="badge-effect">${escapeHtml(e)}</span>`
            ).join('');
            
            const partsHtml = interaction.plantParts && interaction.plantParts.length > 0
                ? `<p><strong>Plant Parts:</strong> ${interaction.plantParts.join(', ')}</p>`
                : '';
            
            const modelHtml = interaction.model ? `<p class="text-muted mb-1"><small><strong>Model:</strong> ${escapeHtml(interaction.model)}</small></p>` : '';
            const articleHtml = interaction.articleTitle ? `<p class="text-muted mb-1"><small><strong>Article:</strong> ${escapeHtml(interaction.articleTitle)}</small></p>` : '';
            
            html += `
                <div class="interaction-card">
                    <h4>${escapeHtml(interaction.plant)} &rarr; ${escapeHtml(interaction.compound)}</h4>
                    ${modelHtml}
                    ${articleHtml}
                    <p><strong>Effects:</strong></p>
                    <div>${effectsHtml}</div>
                    ${partsHtml}
                </div>
            `;
        });
        
        contentDiv.innerHTML = html;
        renderPagination('interactionsPagination', data.totalPages, data.currentPage, data.totalElements, 
            (p) => loadInteractions(p, currentFilters.plantName, currentFilters.compoundName, currentFilters.effect));
        
    } catch (error) {
        contentDiv.innerHTML = `
            <div class="alert alert-danger">
                <strong>Error!</strong> ${error.message}
            </div>
        `;
    }
}

function applyFilters() {
    currentFilters.plantName = document.getElementById('filterPlant').value;
    currentFilters.compoundName = document.getElementById('filterCompound').value;
    currentFilters.effect = document.getElementById('filterEffect').value;
    currentInteractionsPage = 0;
    loadInteractions(0, currentFilters.plantName, currentFilters.compoundName, currentFilters.effect);
}

function clearFilters() {
    document.getElementById('filterPlant').value = '';
    document.getElementById('filterCompound').value = '';
    document.getElementById('filterEffect').value = '';
    currentFilters = { plantName: '', compoundName: '', effect: '' };
    currentInteractionsPage = 0;
    loadInteractions();
}

function renderPagination(elementId, totalPages, currentPage, totalElements, onPageClick) {
    const pagination = document.getElementById(elementId);
    
    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }
    
    let html = '';
    
    // Previous button
    html += `
        <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${currentPage - 1}">Previous</a>
        </li>
    `;
    
    // Page numbers
    for (let i = 0; i < totalPages; i++) {
        if (i === 0 || i === totalPages - 1 || (i >= currentPage - 2 && i <= currentPage + 2)) {
            html += `
                <li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
                </li>
            `;
        } else if (i === currentPage - 3 || i === currentPage + 3) {
            html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
        }
    }
    
    // Next button
    html += `
        <li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${currentPage + 1}">Next</a>
        </li>
    `;
    
    pagination.innerHTML = html;
    
    // Add event listeners to all page links
    pagination.querySelectorAll('.page-link').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const page = parseInt(link.getAttribute('data-page'));
            if (!isNaN(page) && !link.parentElement.classList.contains('disabled')) {
                onPageClick(page);
            }
        });
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function loadArticleSources(articleId, event) {
    event.preventDefault();
    const sourcesDiv = document.getElementById(`sources-${articleId}`);
    
    if (sourcesDiv.innerHTML.trim() !== '') {
        sourcesDiv.innerHTML = '';
        return;
    }
    
    sourcesDiv.innerHTML = '<div class="spinner-border spinner-border-sm" role="status"></div>';
    
    try {
        const response = await fetch(`/api/v1/articles/${articleId}/sources`);
        const sources = await response.json();
        
        if (sources.length === 0) {
            sourcesDiv.innerHTML = '<p class="text-muted">No sources found</p>';
            return;
        }
        
        let html = '<div class="list-group">';
        sources.forEach(source => {
            html += `
                <div class="list-group-item">
                    <h6 class="mb-1">
                        <a href="#" onclick="event.preventDefault(); showSourceDetails(${source.id}); return false;" class="text-decoration-none">
                            ${escapeHtml(source.modelName)}
                        </a>
                    </h6>
                </div>
            `;
        });
        html += '</div>';
        
        sourcesDiv.innerHTML = html;
    } catch (error) {
        sourcesDiv.innerHTML = `
            <div class="alert alert-danger">
                <strong>Error!</strong> ${error.message}
            </div>
        `;
    }
}

async function showSourceDetails(sourceId) {
    const modal = document.getElementById('sourceModal');
    const modalBody = document.getElementById('sourceModalBody');
    const modalTitle = document.getElementById('sourceModalTitle');
    
    if (!modal) {
        createSourceModal();
        await showSourceDetails(sourceId);
        return;
    }
    
    try {
        const response = await fetch(`/api/v1/sources/${sourceId}`);
        const source = await response.json();
        
        modalTitle.textContent = source.modelName;
        modalBody.innerHTML = `
            <pre class="bg-light p-3 rounded border">${escapeHtml(source.rawResponse || 'No raw response available')}</pre>
        `;
        
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();
    } catch (error) {
        modalBody.innerHTML = `
            <div class="alert alert-danger">
                <strong>Error!</strong> ${error.message}
            </div>
        `;
    }
}

function createSourceModal() {
    const modalHtml = `
        <div class="modal fade" id="sourceModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="sourceModalTitle">Source Details</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body" id="sourceModalBody">
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    </div>
                </div>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

