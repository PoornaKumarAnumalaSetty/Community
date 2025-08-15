const $ = (s, c = document) => c.querySelector(s);
const $$ = (s, c = document) => Array.from(c.querySelectorAll(s));

const api = {
    async listIssues() {
        const r = await fetch('/api/issues');
        if (!r.ok) throw new Error('Failed to load');
        return r.json();
    },
    async createIssue(data) {
        const r = await fetch('/api/issues', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams(data).toString(),
        });
        if (!r.ok) throw new Error('Failed to create');
        return r.json();
    },
    async voteIssue(id) {
        const r = await fetch(`/api/issues/${id}/vote`, { method: 'POST' });
        if (!r.ok) throw new Error('Failed to vote');
        return r.json();
    },
    async addComment(id, text) {
        const r = await fetch(`/api/issues/${id}/comments`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ text }).toString(),
        });
        if (!r.ok) throw new Error('Failed to comment');
        return r.json();
    },
};

const state = { issues: [], sort: 'new' };

function sortIssues(issues) {
    if (state.sort === 'top') {
        return [...issues].sort((a, b) => b.votes - a.votes || b.createdAt - a.createdAt);
    }
    return [...issues].sort((a, b) => b.createdAt - a.createdAt);
}

function render() {
    const list = $('#issues');
    list.setAttribute('aria-busy', 'true');
    list.innerHTML = '';
    const tpl = $('#issue-item-template');
    for (const issue of sortIssues(state.issues)) {
        const node = tpl.content.cloneNode(true);
        const li = node.querySelector('li.issue');
        li.dataset.id = issue.id;
        li.querySelector('.votes').textContent = issue.votes;
        li.querySelector('.title').textContent = issue.title;
        li.querySelector('.desc').textContent = issue.description;

        const count = issue.comments?.length || 0;
        li.querySelector('.count').textContent = count;
        const ul = li.querySelector('.comment-list');
        for (const c of issue.comments || []) {
            const item = document.createElement('li');
            item.textContent = c.text;
            ul.appendChild(item);
        }
        list.appendChild(node);
    }
    list.setAttribute('aria-busy', 'false');
}

async function refresh() {
    try {
        const issues = await api.listIssues();
        state.issues = issues;
        render();
    } catch (e) {
        console.error(e);
    }
}

function attachEvents() {
    $('#issue-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const title = $('#title').value.trim();
        const description = $('#description').value.trim();
        if (!title || !description) return;
        $('#title').value = '';
        $('#description').value = '';
        const created = await api.createIssue({ title, description });
        state.issues.push(created);
        render();
    });

    $('.filters').addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-sort]');
        if (!btn) return;
        state.sort = btn.dataset.sort;
        render();
    });

    $('#issues').addEventListener('click', async (e) => {
        const btn = e.target.closest('.vote-btn');
        if (!btn) return;
        const li = e.target.closest('li.issue');
        const id = li?.dataset.id;
        if (!id) return;
        const updated = await api.voteIssue(id);
        const idx = state.issues.findIndex(x => String(x.id) === String(id));
        if (idx >= 0) state.issues[idx] = updated;
        render();
    });

    $('#issues').addEventListener('submit', async (e) => {
        const form = e.target.closest('.comment-form');
        if (!form) return;
        e.preventDefault();
        const li = form.closest('li.issue');
        const id = li?.dataset.id;
        const input = form.querySelector('input[name="text"]');
        const text = input.value.trim();
        if (!text) return;
        input.value = '';
        const updated = await api.addComment(id, text);
        const idx = state.issues.findIndex(x => String(x.id) === String(id));
        if (idx >= 0) state.issues[idx] = updated;
        render();
    });
}

document.addEventListener('DOMContentLoaded', () => {
    attachEvents();
    refresh();
});


