document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const dashboardGrid = document.getElementById('dashboardGrid');
    const uploadZone = document.getElementById('uploadZone');
    const progressSection = document.getElementById('progressSection');
    const transcriptSection = document.getElementById('transcriptSection');
    const summarySection = document.getElementById('summarySection');

    const audioFileInput = document.getElementById('audioFile');
    const uploadArea = document.getElementById('uploadArea');
    const recordBtn = document.getElementById('recordBtn');
    const recordText = document.getElementById('recordText');
    const visualizer = document.getElementById('visualizer');

    const progressFill = document.getElementById('progressFill');
    const progressPhase = document.getElementById('progressPhase');
    const progressText = document.getElementById('progressText');

    const transcriptText = document.getElementById('transcriptText');
    const detectedLang = document.getElementById('detectedLang');
    const summaryContent = document.getElementById('summaryContent');
    const toastContainer = document.getElementById('toastContainer');

    const modelSize = document.getElementById('modelSize');
    const targetLang = document.getElementById('targetLang');

    const historyBtn = document.getElementById('historyBtn');
    const historyModal = document.getElementById('historyModal');
    const closeHistoryBtn = document.getElementById('closeHistoryBtn');
    const historyList = document.getElementById('historyList');
    const saveMeetingBtn = document.getElementById('saveMeetingBtn');

    let mediaRecorder = null;
    let audioChunks = [];
    let currentTranscription = null;
    let currentSummary = null;

    // EVENT LISTENERS
    uploadArea.addEventListener('dragover', (e) => { e.preventDefault(); uploadArea.classList.add('drag-over'); });
    uploadArea.addEventListener('dragleave', () => uploadArea.classList.remove('drag-over'));
    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault(); uploadArea.classList.remove('drag-over');
        if (e.dataTransfer.files.length && e.dataTransfer.files[0].type.startsWith('audio/')) {
            processAudio(e.dataTransfer.files[0]);
        } else {
            showToast('Unsupported file type. Please upload audio.', 'error');
        }
    });

    uploadArea.addEventListener('click', (e) => {
        if (e.target.tagName.toLowerCase() !== 'label' && e.target.htmlFor !== 'audioFile') {
            audioFileInput.click();
        }
    });

    audioFileInput.addEventListener('change', (e) => {
        if (e.target.files.length) processAudio(e.target.files[0]);
    });

    recordBtn.addEventListener('click', toggleRecording);

    document.getElementById('newMeetingBtn').addEventListener('click', resetDashboard);
    document.getElementById('exportPdfBtn').addEventListener('click', exportToPDF);
    saveMeetingBtn.addEventListener('click', saveMeetingToHistory);

    historyBtn.addEventListener('click', openHistory);
    closeHistoryBtn.addEventListener('click', () => historyModal.classList.add('hidden'));
    historyModal.addEventListener('click', (e) => {
        if (e.target === historyModal) historyModal.classList.add('hidden');
    });

    document.querySelectorAll('[data-copy]').forEach(btn => {
        btn.addEventListener('click', (e) => handleCopy(e.currentTarget));
    });

    // FUNCTIONS
    async function toggleRecording() {
        if (mediaRecorder && mediaRecorder.state === 'recording') {
            mediaRecorder.stop();
            recordBtn.classList.remove('recording');
            recordText.textContent = "Live Record Meeting";
            visualizer.classList.add('hidden');
        } else {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
                mediaRecorder = new MediaRecorder(stream);
                audioChunks = [];
                mediaRecorder.ondataavailable = e => audioChunks.push(e.data);
                mediaRecorder.onstop = () => {
                    stream.getTracks().forEach(t => t.stop());
                    const file = new File([new Blob(audioChunks)], 'live_recording.webm', { type: 'audio/webm' });
                    processAudio(file);
                };
                mediaRecorder.start();
                recordBtn.classList.add('recording');
                recordText.textContent = "Stop Recording...";
                visualizer.classList.remove('hidden');
            } catch (err) {
                showToast('Microphone access denied.', 'error');
            }
        }
    }

    async function processAudio(file) {
        showProgressView();

        const fd = new FormData();
        fd.append('audio', file);
        fd.append('model', modelSize.value);

        try {
            updateProgress(15, 'Pipeline Connected', 'Uploading audio for transcription...');
            const trRes = await fetch('/api/transcribe', { method: 'POST', body: fd });
            if (!trRes.ok) throw new Error((await trRes.json()).error || 'Transcription failed');

            const trData = await trRes.json();
            currentTranscription = trData;

            updateProgress(65, 'Transcription Complete', 'Generating executive meeting minutes...');
            const sumRes = await fetch('/api/summarize', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    text: trData.text,
                    language: trData.language,
                    target_lang: targetLang.value
                })
            });

            if (!sumRes.ok) throw new Error((await sumRes.json()).error || 'Summarization failed');
            currentSummary = await sumRes.json();

            updateProgress(100, 'Intelligence Rendered', 'Finalizing layout...');
            setTimeout(() => showDashboard(trData, currentSummary), 800);

        } catch (err) {
            resetDashboard();
            showToast(err.message, 'error');
        }
    }

    function showProgressView() {
        uploadZone.classList.add('hidden');
        progressSection.classList.remove('hidden');
        transcriptSection.classList.add('hidden');
        summarySection.classList.add('hidden');
        updateProgress(0, 'Initializing', 'Preparing upload...');
    }

    function updateProgress(percent, phase, text) {
        progressFill.style.width = percent + '%';
        progressPhase.textContent = phase;
        progressText.textContent = text;
    }

    function showDashboard(trData, sumData) {
        progressSection.classList.add('hidden');
        uploadZone.classList.add('hidden');

        // Morph Grid
        dashboardGrid.classList.remove('state-initial');
        dashboardGrid.classList.add('state-split');

        transcriptSection.classList.remove('hidden');
        summarySection.classList.remove('hidden');

        detectedLang.textContent = trData.original_audio_language ? trData.original_audio_language.toUpperCase() : 'EN';
        transcriptText.textContent = trData.text;

        summaryContent.innerHTML = sumData.professional_minutes ? marked.parse(sumData.professional_minutes) : '<p>Summary failed.</p>';
        showToast('Meeting intelligence extracted successfully.', 'success');
    }

    function resetDashboard() {
        dashboardGrid.classList.remove('state-split');
        dashboardGrid.classList.add('state-initial');
        uploadZone.classList.remove('hidden');
        progressSection.classList.add('hidden');
        transcriptSection.classList.add('hidden');
        summarySection.classList.add('hidden');
        audioFileInput.value = '';
        currentTranscription = null;
        currentSummary = null;
    }

    function handleCopy(btn) {
        const targetId = btn.dataset.copy;
        const text = targetId === 'transcriptText' ? transcriptText.textContent : currentSummary?.professionalMinutes || currentSummary?.professional_minutes;
        if (!text) return;

        navigator.clipboard.writeText(text).then(() => {
            showToast('Copied to clipboard', 'success');
        });
    }

    function exportToPDF() {
        if (!currentSummary) return showToast('No data to export', 'error');
        const d = new Date().toISOString().split('T')[0];
        const filename = `Minutes_${d}.pdf`;

        const html = `
            <div style="padding:40px; font-family:Helvetica; color:#000; background:#fff;">
                <h1 style="color:#000; border-bottom:1px solid #ccc;">Meeting Minutes (${d})</h1>
                ${marked.parse(currentSummary.professional_minutes)}
            </div>
        `;

        showToast('Generating PDF...', 'info');
        const btn = document.getElementById('exportPdfBtn');
        const originalText = btn.innerHTML;
        btn.innerHTML = 'Exporting...';
        btn.disabled = true;

        const opt = {
            margin: 0.5,
            filename: filename,
            image: { type: 'jpeg', quality: 0.98 },
            html2canvas: { scale: 2, useCORS: true, backgroundColor: '#ffffff' },
            jsPDF: { unit: 'in', format: 'letter', orientation: 'portrait' }
        };

        const container = document.createElement('div');
        container.innerHTML = html;

        // PDF export happens entirely in the browser and downloads directly -
        // no server round-trip or server-side file writing required.
        html2pdf().set(opt).from(container).save().then(() => {
            showToast('PDF downloaded!', 'success');
            btn.innerHTML = originalText;
            btn.disabled = false;
        }).catch(err => {
            console.error(err);
            showToast('Failed to generate PDF', 'error');
            btn.innerHTML = originalText;
            btn.disabled = false;
        });
    }

    async function saveMeetingToHistory() {
        if (!currentTranscription || !currentSummary) {
            return showToast('Nothing to save yet', 'error');
        }

        const d = new Date().toLocaleString();
        const title = `Meeting - ${d}`;

        try {
            const res = await fetch('/api/meetings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    title: title,
                    transcript: currentTranscription.text,
                    minutesMarkdown: currentSummary.professional_minutes,
                    language: currentSummary.language
                })
            });

            if (!res.ok) throw new Error((await res.json()).error || 'Failed to save meeting');
            showToast('Meeting saved to history', 'success');
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    async function openHistory() {
        historyModal.classList.remove('hidden');
        historyList.innerHTML = '<p class="history-empty">Loading...</p>';

        try {
            const res = await fetch('/api/meetings');
            if (!res.ok) throw new Error('Failed to load history');
            const meetings = await res.json();
            renderHistory(meetings);
        } catch (err) {
            historyList.innerHTML = `<p class="history-empty">${err.message}</p>`;
        }
    }

    function renderHistory(meetings) {
        if (!meetings.length) {
            historyList.innerHTML = '<p class="history-empty">No saved meetings yet. Generate some minutes and hit "Save to History".</p>';
            return;
        }

        historyList.innerHTML = '';
        meetings.forEach(m => {
            const item = document.createElement('div');
            item.className = 'history-item';
            const date = new Date(m.createdAt).toLocaleString();
            item.innerHTML = `
                <div>
                    <div class="history-item-title">${escapeHtml(m.title)}</div>
                    <div class="history-item-meta">${date} &middot; ${m.language.toUpperCase()}</div>
                </div>
                <button class="history-item-delete" data-id="${m.id}" title="Delete">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                </button>
            `;
            item.addEventListener('click', (e) => {
                if (e.target.closest('.history-item-delete')) return;
                loadMeeting(m.id);
            });
            item.querySelector('.history-item-delete').addEventListener('click', async (e) => {
                e.stopPropagation();
                await deleteMeeting(m.id);
            });
            historyList.appendChild(item);
        });
    }

    async function loadMeeting(id) {
        try {
            const res = await fetch(`/api/meetings/${id}`);
            if (!res.ok) throw new Error('Failed to load meeting');
            const meeting = await res.json();

            currentTranscription = { text: meeting.transcript, language: meeting.language, original_audio_language: meeting.language };
            currentSummary = { professional_minutes: meeting.minutesMarkdown, language: meeting.language };

            historyModal.classList.add('hidden');
            showDashboard(currentTranscription, currentSummary);
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    async function deleteMeeting(id) {
        try {
            const res = await fetch(`/api/meetings/${id}`, { method: 'DELETE' });
            if (!res.ok && res.status !== 204) throw new Error('Failed to delete meeting');
            showToast('Meeting deleted', 'success');
            openHistory();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    function showToast(msg, type='info') {
        const t = document.createElement('div');
        t.style.cssText = `
            background: ${type==='error'?'#ef4444':'var(--neon-cyan)'};
            color: ${type==='error'?'#fff':'#000'};
            padding: 12px 24px; border-radius: 8px; font-weight: 500;
            margin-top: 10px; opacity: 0; transform: translateY(20px);
            transition: all 0.3s;
        `;
        t.textContent = msg;
        toastContainer.appendChild(t);

        requestAnimationFrame(() => { t.style.opacity=1; t.style.transform='translateY(0)'; });
        setTimeout(() => {
            t.style.opacity = 0;
            setTimeout(() => t.remove(), 300);
        }, 3000);
    }
});
