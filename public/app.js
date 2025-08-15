// Senior-Friendly Community Board JavaScript
const $ = (s, c = document) => c.querySelector(s);
const $$ = (s, c = document) => Array.from(c.querySelectorAll(s));

// API functions with better error handling
const api = {
    async listIssues() {
        try {
            const r = await fetch('/api/issues');
            if (!r.ok) throw new Error(`Failed to load posts (${r.status})`);
            return r.json();
        } catch (e) {
            showMessage('❌ Unable to load community posts. Please try again.', 'error');
            throw e;
        }
    },

    async createIssue(data) {
        try {
            const r = await fetch('/api/issues', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams(data).toString(),
            });
            if (!r.ok) throw new Error(`Failed to create post (${r.status})`);
            showMessage('✅ Your post has been shared with the community!', 'success');
            return r.json();
        } catch (e) {
            showMessage('❌ Unable to share your post. Please try again.', 'error');
            throw e;
        }
    },

    async voteIssue(id) {
        try {
            const r = await fetch(`/api/issues/${id}/vote`, { method: 'POST' });
            if (!r.ok) throw new Error(`Failed to support post (${r.status})`);
            showMessage('👍 Thank you for supporting this idea!', 'success');
            return r.json();
        } catch (e) {
            showMessage('❌ Unable to support this post. Please try again.', 'error');
            throw e;
        }
    },

    async addComment(id, text) {
        try {
            const r = await fetch(`/api/issues/${id}/comments`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({ text }).toString(),
            });
            if (!r.ok) throw new Error(`Failed to add comment (${r.status})`);
            showMessage('💬 Your comment has been added!', 'success');
            return r.json();
        } catch (e) {
            showMessage('❌ Unable to add comment. Please try again.', 'error');
            throw e;
        }
    },
};

// Application state
const state = { 
    issues: [], 
    sort: 'new',
    loading: true
};

// Utility functions
function formatDate(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;
    
    if (diff < 60000) return 'Just now';
    if (diff < 3600000) return `${Math.floor(diff / 60000)} minutes ago`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)} hours ago`;
    if (diff < 604800000) return `${Math.floor(diff / 86400000)} days ago`;
    
    return date.toLocaleDateString('en-US', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric' 
    });
}

function showMessage(text, type = 'info') {
    // Remove existing messages
    const existing = $('.message');
    if (existing) existing.remove();
    
    const message = document.createElement('div');
    message.className = `message message-${type}`;
    message.textContent = text;
    
    // Add styles
    message.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        border-radius: 8px;
        color: white;
        font-size: 16px;
        font-weight: 600;
        z-index: 1000;
        max-width: 400px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        animation: slideIn 0.3s ease;
    `;
    
    if (type === 'success') message.style.background = '#198754';
    else if (type === 'error') message.style.background = '#dc3545';
    else message.style.background = '#0d6efd';
    
    document.body.appendChild(message);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (message.parentNode) {
            message.style.animation = 'slideOut 0.3s ease';
            setTimeout(() => message.remove(), 300);
        }
    }, 5000);
}

// Sorting functions
function sortIssues(issues) {
    if (state.sort === 'top') {
        return [...issues].sort((a, b) => b.votes - a.votes || b.createdAt - a.createdAt);
    }
    return [...issues].sort((a, b) => b.createdAt - a.createdAt);
}

// Render functions
function render() {
    const list = $('#issues');
    const loading = $('#loading');
    
    if (state.loading) {
        list.style.display = 'none';
        loading.style.display = 'block';
        return;
    }
    
    list.style.display = 'grid';
    loading.style.display = 'none';
    
    list.setAttribute('aria-busy', 'true');
    list.innerHTML = '';
    
    const tpl = $('#issue-item-template');
    
    if (state.issues.length === 0) {
        const emptyMessage = document.createElement('div');
        emptyMessage.className = 'empty-message';
        emptyMessage.innerHTML = `
            <div style="text-align: center; padding: 60px 20px; color: #6c757d;">
                <div style="font-size: 48px; margin-bottom: 20px;">📝</div>
                <h3 style="font-size: 24px; margin: 0 0 15px;">No posts yet</h3>
                <p style="font-size: 18px; margin: 0;">Be the first to share something with your community!</p>
            </div>
        `;
        list.appendChild(emptyMessage);
    } else {
        for (const issue of sortIssues(state.issues)) {
            const node = tpl.content.cloneNode(true);
            const li = node.querySelector('li.issue');
            
            li.dataset.id = issue.id;
            li.querySelector('.votes').textContent = issue.votes;
            li.querySelector('.title').textContent = issue.title;
            li.querySelector('.desc').textContent = issue.description;
            li.querySelector('.date').textContent = formatDate(issue.createdAt);
            li.querySelector('.supporters').textContent = `${issue.votes} supporter${issue.votes !== 1 ? 's' : ''}`;

            const count = issue.comments?.length || 0;
            li.querySelector('.count').textContent = count;
            
            const ul = li.querySelector('.comment-list');
            for (const c of issue.comments || []) {
                const item = document.createElement('li');
                item.innerHTML = `
                    <div style="margin-bottom: 10px; color: #6c757d; font-size: 14px;">
                        ${formatDate(c.createdAt)}
                    </div>
                    <div>${c.text}</div>
                `;
                ul.appendChild(item);
            }
            
            list.appendChild(node);
        }
    }
    
    list.setAttribute('aria-busy', 'false');
}

// Data loading
async function refresh() {
    try {
        state.loading = true;
        render();
        
        const issues = await api.listIssues();
        state.issues = issues;
        state.loading = false;
        render();
    } catch (e) {
        state.loading = false;
        render();
        console.error('Failed to refresh:', e);
    }
}

// Event handlers
function attachEvents() {
    // Form submission
    $('#issue-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const title = $('#title').value.trim();
        const description = $('#description').value.trim();
        
        if (!title || !description) {
            showMessage('❌ Please fill in both title and description.', 'error');
            return;
        }
        
        // Disable form during submission
        const submitBtn = $('#issue-form button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = '📝 Sharing...';
        
        try {
            const created = await api.createIssue({ title, description });
            state.issues.push(created);
            
            // Clear form
            $('#title').value = '';
            $('#description').value = '';
            
            render();
        } catch (e) {
            console.error('Failed to create issue:', e);
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
        }
    });

    // Sorting filters
    $('.filters').addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-sort]');
        if (!btn) return;
        
        // Update active state
        $$('.btn-filter').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        
        state.sort = btn.dataset.sort;
        render();
    });

    // Voting
    $('#issues').addEventListener('click', async (e) => {
        const btn = e.target.closest('.btn-vote');
        if (!btn) return;
        
        const li = e.target.closest('li.issue');
        const id = li?.dataset.id;
        if (!id) return;
        
        // Disable button during request
        btn.disabled = true;
        btn.style.opacity = '0.6';
        
        try {
            const updated = await api.voteIssue(id);
            const idx = state.issues.findIndex(x => String(x.id) === String(id));
            if (idx >= 0) state.issues[idx] = updated;
            render();
        } catch (e) {
            console.error('Failed to vote:', e);
        } finally {
            btn.disabled = false;
            btn.style.opacity = '1';
        }
    });

    // Comments
    $('#issues').addEventListener('submit', async (e) => {
        const form = e.target.closest('.comment-form');
        if (!form) return;
        
        e.preventDefault();
        
        const li = form.closest('li.issue');
        const id = li?.dataset.id;
        const input = form.querySelector('input[name="text"]');
        const text = input.value.trim();
        
        if (!text) {
            showMessage('❌ Please enter a comment.', 'error');
            return;
        }
        
        // Disable form during submission
        const submitBtn = form.querySelector('button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = '💬 Adding...';
        
        try {
            const updated = await api.addComment(id, text);
            const idx = state.issues.findIndex(x => String(x.id) === String(id));
            if (idx >= 0) state.issues[idx] = updated;
            
            input.value = '';
            render();
        } catch (e) {
            console.error('Failed to add comment:', e);
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
        }
    });
}

// Initialize app
document.addEventListener('DOMContentLoaded', () => {
    attachEvents();
    refresh();
    
    // Add CSS animations
    const style = document.createElement('style');
    style.textContent = `
        @keyframes slideIn {
            from { transform: translateX(100%); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }
        @keyframes slideOut {
            from { transform: translateX(0); opacity: 1; }
            to { transform: translateX(100%); opacity: 0; }
        }
    `;
    document.head.appendChild(style);
});
