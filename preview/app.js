const STORAGE_KEY = 'kinvoice-web-v3';

const seed = {
  tab: 'library',
  query: '',
  filter: '全部',
  recording: false,
  recordingStarted: 0,
  mediaRecorder: null,
  recordingStream: null,
  audioChunks: [],
  recordedAudioURL: '',
  memories: [
    { id: 'm1', title: '外婆的红烧肉：先炒糖色，再等香气', author: '林秀兰', summary: '一道从曾外婆传下来的年夜饭做法。', time: '1978 年除夕', place: '苏州', quote: '糖色不能急，闻到焦糖香才算到时候。', content: '外婆说，糖色不能急。冰糖在小火里慢慢化开，颜色像老茶时才下肉。肉要先擦干水，不然油会溅。加热水没过一半，最后十分钟再放盐。\n\n她年轻时第一次掌勺是在 1978 年的除夕，这道菜后来成了家里的年夜饭。', tags: ['家常菜', '年夜饭'], status: 'confirmed', audio: true },
    { id: 'm2', title: '旧缝纫机旁学会的耐心', author: '周明芳', summary: '母亲教女儿做衣服，也教她遇事先量清楚。', time: '1980 年代', place: '无锡', quote: '剪刀落下去就不能反悔，先量清楚。', content: '妈妈踩缝纫机前，总会把布铺平量三遍。她说剪刀落下去就不能反悔，生活里许多事也是这样。\n\n那台缝纫机是她结婚时买的，后来给家里四个孩子做过棉衣。', tags: ['手艺', '家训'], status: 'confirmed', audio: false },
    { id: 'm3', title: '爷爷第一次坐火车去上海', author: '陈国安', summary: '一张硬座票和一只搪瓷杯，装着第一次远行的忐忑。', time: '1966 年夏', place: '上海', quote: '车窗外的电线杆一根接一根，像在带我往前走。', content: '爷爷十八岁第一次坐火车去上海学修机器。奶奶给他装了三个饭团，他一路舍不得吃完。到了上海以后，他把那张硬座票夹进字典里，提醒自己学会一门真正能帮到别人的手艺。', tags: ['远行', '成长'], status: 'confirmed', audio: false },
    { id: 'm4', title: '端午香囊里的草木配方', author: '林秀兰', summary: '外婆每年端午都会教孩子辨认艾叶、薄荷和藿香。', time: '每年端午', place: '苏州', quote: '先闻，再认叶子的边，草木才记得牢。', content: '外婆会把晒干的艾叶、薄荷和藿香分成小碟，让孩子蒙着眼睛闻。认对以后，再一起装进布袋缝好。她说配方不是最重要的，记住季节和一家人一起做香囊的过程才重要。', tags: ['节气', '手艺'], status: 'confirmed', audio: true },
    { id: 'm5', title: '爸爸教我的第一张全家福', author: '陈建平', summary: '不是看镜头，而是先让每个人都放松下来。', time: '2003 年春节', place: '杭州', quote: '好照片不是把人排整齐，是把那一刻留下来。', content: '爸爸把胶片相机交给我时，只讲了光圈和快门最基本的用法。他更在意我有没有看到每个人当时的样子。那张全家福里，小姨正在笑，外公还没来得及看镜头，后来反而成了大家最喜欢的一张。', tags: ['影像', '家庭'], status: 'needs-review', audio: false },
    { id: 'm6', title: '家里第一台收音机', author: '陈建平', summary: '一家人围在窗边听天气预报的那个夏天。', time: '1972 年夏', place: '杭州', quote: '声音一响，屋子里就像多了一个远方的朋友。', content: '那台收音机是舅舅从上海带回来的。晚饭后大家把椅子搬到窗边，听天气预报和评书。', tags: ['旧物', '家庭'], status: 'needs-review', audio: false },
    { id: 'm7', title: '外公的木工尺', author: '赵世宁', summary: '一把旧木尺，记录着他做家具时的规矩。', time: '1960 年代', place: '嘉兴', quote: '木头会记住你下过的每一刀。', content: '外公的木工尺上有一道很浅的刻痕，他说那是第一张书桌留下的。', tags: ['手艺', '旧物'], status: 'confirmed', audio: false },
    { id: 'm8', title: '家门口的桂花树', author: '周明芳', summary: '每年秋天，全家一起收桂花做糖。', time: '每年秋天', place: '无锡', quote: '香味先到，家就先回来了。', content: '门口的桂花树陪着孩子们长大。落花的时候，大家拿竹筛收集，晒干后装进玻璃罐。', tags: ['节气', '家园'], status: 'confirmed', audio: false }
  ],
  people: [
    { id: 'p1', name: '林秀兰', relation: '外婆', role: '讲述者', bio: '擅长家常菜、节气和苏州旧事。' },
    { id: 'p2', name: '周明芳', relation: '妈妈', role: '讲述者', bio: '家里的手艺传承人。' },
    { id: 'p3', name: '陈国安', relation: '爷爷', role: '讲述者', bio: '经历过第一次远行和工厂岁月。' },
    { id: 'p4', name: '陈建平', relation: '爸爸', role: '编辑者', bio: '负责整理照片和旧物故事。' }
  ],
  prompts: [
    '小时候家里过年，最不能少的一道菜是什么？',
    '你年轻时学会的第一门手艺是什么？',
    '这道家常菜是谁教你的？',
    '家里有哪条规矩，背后藏着什么故事？',
    '你第一次离开家去远方时，记得什么？'
  ]
};

function loadState() {
  const defaults = JSON.parse(JSON.stringify(seed));
  try { return { ...defaults, ...JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') }; } catch { return defaults; }
}
const state = loadState();
const app = document.querySelector('#app');
const dialog = document.querySelector('#dialog');
const dialogContent = document.querySelector('#dialog-content');
const toast = document.querySelector('#toast');

function esc(value = '') { return String(value).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
function persist() {
  const data = { memories: state.memories, people: state.people, prompts: state.prompts };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}
function notify(message) { toast.textContent = message; toast.classList.add('show'); clearTimeout(notify.timer); notify.timer = setTimeout(() => toast.classList.remove('show'), 2200); }
function personByName(name) { return state.people.find(p => p.name === name); }
function formatDate() { return new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric' }).format(new Date()); }
function statusLabel(status) { return status === 'confirmed' ? '家人已确认' : '待校订'; }
function statusClass(status) { return status === 'confirmed' ? 'confirmed' : ''; }
function closeDialog() { if (dialog.open) dialog.close(); }

function render() {
  const titleMap = { library: ['家庭知识传承库', '记忆库'], capture: ['引导式采访', '采集口述'], ask: ['仅依据家庭资料', '问家'], family: ['成员与数据', '家庭协作'] };
  const [eyebrow, title] = titleMap[state.tab];
  app.innerHTML = `<div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><img src="../ios/Assets.xcassets/AppIcon.appiconset/AppIcon-1024-opaque.png" alt="家声"><div><strong>KinVoice 家声</strong><small>家庭知识传承库</small></div></div>
      <div class="nav-label">工作空间</div>
      <nav class="nav-list" aria-label="主要页面">${navItem('library', '▥', '记忆库', '家庭知识资产')} ${navItem('capture', '●', '采集口述', '采访与录音')} ${navItem('ask', '◌', '问家', '带来源检索')} ${navItem('family', '♧', '家庭协作', '成员与授权')}</nav>
      <div class="sidebar-foot"><span class="status-dot"></span>本地体验模式<br>数据保存在当前浏览器</div>
    </aside>
    <main class="workspace">
      <header class="topbar"><div class="mobile-brand"><img src="../ios/Assets.xcassets/AppIcon.appiconset/AppIcon-1024-opaque.png" alt=""><span>家声</span></div><div class="breadcrumb"><small>${eyebrow}</small><strong>${title}</strong></div><div class="top-actions"><button class="secondary desktop-only" id="export-data">导出家庭资料</button><button class="icon-button" id="reset-data" title="恢复演示数据" aria-label="恢复演示数据">↺</button></div></header>
      <div class="content" id="content"></div>
    </main>
    <nav class="mobile-tabs" aria-label="主要页面">${navItem('library', '▥', '记忆库')} ${navItem('capture', '●', '采集')} ${navItem('ask', '◌', '问家')} ${navItem('family', '♧', '家庭')}</nav>
  </div>`;
  document.querySelectorAll('[data-tab]').forEach(button => button.addEventListener('click', () => { state.tab = button.dataset.tab; state.query = ''; render(); }));
  document.querySelector('#export-data').addEventListener('click', exportData);
  document.querySelector('#reset-data').addEventListener('click', resetData);
  const content = document.querySelector('#content');
  if (state.tab === 'library') renderLibrary(content);
  if (state.tab === 'capture') renderCapture(content);
  if (state.tab === 'ask') renderAsk(content);
  if (state.tab === 'family') renderFamily(content);
}

function navItem(tab, icon, label, sub = '') { return `<button class="nav-item ${state.tab === tab ? 'active' : ''}" data-tab="${tab}"><span class="nav-icon">${icon}</span><span><strong>${label}</strong>${sub ? `<small>${sub}</small>` : ''}</span></button>`; }
function pageIntro(eyebrow, title, desc, action = '') { return `<div class="page-intro"><div><div class="eyebrow">${eyebrow}</div><h1>${title}</h1><p>${desc}</p></div>${action}</div>`; }
function filteredMemories() {
  const query = state.query.trim().toLocaleLowerCase('zh-CN');
  return state.memories.filter(m => {
    const haystack = `${m.title}${m.author}${m.summary}${m.content}${m.quote}${m.place}${m.tags.join('')}`.toLocaleLowerCase('zh-CN');
    const matchesQuery = !query || haystack.includes(query);
    const matchesFilter = state.filter === '全部' || (state.filter === '待校订' ? m.status !== 'confirmed' : m.tags.includes(state.filter));
    return matchesQuery && matchesFilter;
  });
}

function renderLibrary(root) {
  const memories = filteredMemories();
  const confirmed = state.memories.filter(m => m.status === 'confirmed').length;
  const people = new Set(state.memories.map(m => m.author)).size;
  root.innerHTML = `${pageIntro('家庭知识传承库', '记忆库', '把家人的声音、经验和手艺，留给下一代。', '<button class="primary" id="quick-capture">＋ 开始一次采访</button>')}
    <div class="stats"><div class="stat"><small>已保存记忆</small><strong>${state.memories.length}</strong><span>条家庭内容</span></div><div class="stat"><small>家人来源</small><strong>${people}</strong><span>位讲述者</span></div><div class="stat"><small>已确认</small><strong>${confirmed}</strong><span>条可放心引用</span></div><div class="stat"><small>共同校订</small><strong>${Math.min(confirmed, 4)}</strong><span>次家庭参与</span></div></div>
    <div class="toolbar"><div class="search"><input id="library-search" aria-label="搜索家庭记忆" placeholder="搜索人物、地点、手艺或原话" value="${esc(state.query)}"></div><div class="filter-row">${['全部', '待校订', '手艺', '节气', '旧物'].map(f => `<button class="chip ${state.filter === f ? 'active' : ''}" data-filter="${f}">${f}</button>`).join('')}</div></div>
    <div class="section-head"><h2>${state.query || state.filter !== '全部' ? '筛选结果' : '最近保存'}</h2><span>${memories.length} 条</span></div><div class="memory-grid">${memories.map(memoryCard).join('') || '<div class="empty">没有找到相关记忆<br><small>换个人物、地点或原话试试</small></div>'}</div>`;
  root.querySelector('#quick-capture').onclick = () => { state.tab = 'capture'; render(); };
  root.querySelector('#library-search').oninput = e => { state.query = e.target.value; renderLibrary(root); };
  root.querySelectorAll('[data-filter]').forEach(b => b.onclick = () => { state.filter = b.dataset.filter; renderLibrary(root); });
  root.querySelectorAll('[data-memory]').forEach(b => b.onclick = () => showMemory(b.dataset.memory));
}

function memoryCard(m) { return `<button class="memory-card" data-memory="${m.id}"><div class="memory-top"><span class="person-pill">${esc(m.author)} · ${esc(m.time)}</span><span class="verify ${statusClass(m.status)}">${statusLabel(m.status)}</span></div><h3>${esc(m.title)}</h3><p>${esc(m.summary)}</p><div class="memory-meta"><span>${m.audio ? '◉ 有原声' : '文字记录'}${m.place ? ` · ${esc(m.place)}` : ''}</span><span class="tags">${m.tags.slice(0, 2).map(t => `<span class="tag">#${esc(t)}</span>`).join('')}</span></div></button>`; }

function showMemory(id) {
  const memory = state.memories.find(m => m.id === id); if (!memory) return;
  dialogContent.innerHTML = `<div class="dialog-inner"><div class="dialog-head"><div><h2>${esc(memory.title)}</h2><div class="detail-meta"><span>${esc(memory.author)}</span><span>${esc(memory.time)}</span><span>${esc(memory.place || '地点未记录')}</span><span>${statusLabel(memory.status)}</span></div></div><button class="icon-button" id="close-dialog" aria-label="关闭">×</button></div>${memory.quote ? `<div class="quote">“${esc(memory.quote)}”</div>` : ''}<div class="article">${esc(memory.content)}</div><div class="tags" style="margin-top:18px">${memory.tags.map(t => `#${esc(t)}　`).join('')}</div>${memory.audio ? '<p class="hint">◉ 这条记忆有原声版本（演示数据仅展示状态）</p>' : ''}<div class="dialog-actions"><button class="secondary" id="speak">▶ 系统朗读</button><button class="secondary" id="edit">编辑记忆</button><button class="primary" id="done">完成</button></div></div>`;
  dialog.showModal();
  dialogContent.querySelector('#close-dialog').onclick = closeDialog;
  dialogContent.querySelector('#done').onclick = closeDialog;
  dialogContent.querySelector('#speak').onclick = () => { speechSynthesis.cancel(); speechSynthesis.speak(new SpeechSynthesisUtterance(memory.content)); notify('正在使用系统语音朗读'); };
  dialogContent.querySelector('#edit').onclick = () => showMemoryForm(memory.id);
}

function showMemoryForm(id = '') {
  const m = state.memories.find(x => x.id === id);
  dialogContent.innerHTML = `<form class="dialog-inner" id="memory-form"><div class="dialog-head"><div><h2>${m ? '编辑记忆' : '新增记忆'}</h2><p>AI 只负责整理，保存前请由家人确认事实。</p></div><button type="button" class="icon-button" id="close-dialog">×</button></div><div class="form-grid"><div class="form-field"><label for="memory-title">标题</label><input id="memory-title" name="title" required maxlength="80" value="${esc(m?.title || '')}" placeholder="例如：外婆的红烧肉"></div><div class="form-field"><label for="memory-author">讲述人</label><input id="memory-author" name="author" required value="${esc(m?.author || '林秀兰')}" placeholder="例如：林秀兰"></div><div class="form-field"><label for="memory-time">时间线索</label><input id="memory-time" name="time" value="${esc(m?.time || '')}" placeholder="例如：1978 年除夕"></div><div class="form-field"><label for="memory-place">地点</label><input id="memory-place" name="place" value="${esc(m?.place || '')}" placeholder="例如：苏州"></div><div class="form-field span-2"><label for="memory-summary">摘要</label><input id="memory-summary" name="summary" value="${esc(m?.summary || '')}" placeholder="用一句话说清这段记忆"></div><div class="form-field span-2"><label for="memory-quote">原话</label><input id="memory-quote" name="quote" value="${esc(m?.quote || '')}" placeholder="最值得保留的一句话"></div><div class="form-field span-2"><label for="memory-content">正文</label><textarea id="memory-content" name="content" required>${esc(m?.content || '')}</textarea></div></div><div class="dialog-actions">${m ? '<button type="button" class="danger" id="delete-memory">删除</button>' : ''}<button type="button" class="secondary" id="cancel">取消</button><button class="primary">保存记忆</button></div></form>`;
  dialog.showModal();
  dialogContent.querySelector('#close-dialog').onclick = closeDialog;
  dialogContent.querySelector('#cancel').onclick = closeDialog;
  if (m) dialogContent.querySelector('#delete-memory').onclick = () => { if (confirm('确定删除这条家庭记忆？')) { state.memories = state.memories.filter(x => x.id !== m.id); persist(); closeDialog(); render(); notify('记忆已删除'); } };
  dialogContent.querySelector('#memory-form').onsubmit = e => { e.preventDefault(); const form = new FormData(e.currentTarget); const data = Object.fromEntries(form.entries()); const next = { ...m, ...data, id: m?.id || `m${Date.now()}`, tags: m?.tags || ['新记忆'], status: m?.status || 'needs-review', audio: m?.audio || false }; if (m) Object.assign(m, next); else state.memories.unshift(next); persist(); closeDialog(); render(); notify(m ? '记忆已更新' : '记忆已保存'); };
}

function renderCapture(root) {
  const prompt = state.prompts[0] || '今天想从哪一段故事开始？';
  root.innerHTML = `${pageIntro('引导式采访', '采集口述', '用一个好问题，换来一段可以留给下一代的真实声音。')}
    <div class="two-column"><section class="panel"><div class="panel-title"><h2>一次采访</h2><span>本地录音 · 可随时暂停</span></div><div class="form-field"><label for="narrator">这次和谁聊</label><input id="narrator" value="${esc(state.people[0]?.name || '')}" placeholder="例如：外婆"></div><div class="form-field"><label for="chosen-prompt">采访问题（可直接编辑）</label><textarea class="question-box" id="chosen-prompt">${esc(prompt)}</textarea><div class="prompt-list" id="prompt-list">${state.prompts.slice(1).map(p => `<button class="prompt" data-prompt="${esc(p)}">${esc(p)}</button>`).join('')}</div><button class="text-button" id="edit-prompts">编辑问题库 →</button></div><div class="form-field"><label>口述记录</label><div class="recording-area"><button class="record-button" id="record" aria-label="开始录音">●</button><div class="record-time" id="record-time">00:00</div><div class="record-state" id="record-state">点击开始录音，也可以直接输入文字</div><div id="recording-result">${state.recordedAudioURL ? '<audio controls src="' + state.recordedAudioURL + '"></audio>' : ''}</div></div></div><div class="form-field"><label for="transcript">文字草稿</label><textarea id="transcript" placeholder="录音后可粘贴或修改文字，整理时会保留原始措辞。">${esc(state.captureText || '')}</textarea></div><button class="primary" id="organize">✦ 整理成记忆草稿</button></section><aside class="side-guide"><section class="panel"><div class="panel-title"><h2>采访节奏</h2><span>3 个动作</span></div><div class="step-list"><div class="step active"><span class="step-mark">1</span><div><strong>先问一个具体问题</strong><small>从一道菜、一件旧物或一次远行开始，避免“讲讲你的一生”。</small></div></div><div class="step"><span class="step-mark">2</span><div><strong>保留原声和停顿</strong><small>录音是证据，也是情感。文字只作为更容易检索的入口。</small></div></div><div class="step"><span class="step-mark">3</span><div><strong>由家人确认</strong><small>AI 生成的是草稿，事实、称呼和授权权利由家人决定。</small></div></div></div></section><section class="panel"><div class="panel-title"><h2>今日提示</h2></div><p class="hint">${formatDate()} 适合问：家里有没有一件旧物，大家一看到就会想起一个人？</p></section></aside></div>`;
  root.querySelector('#transcript').oninput = e => { state.captureText = e.target.value; };
  root.querySelectorAll('[data-prompt]').forEach(b => b.onclick = () => { root.querySelector('#chosen-prompt').value = b.dataset.prompt; });
  root.querySelector('#edit-prompts').onclick = showPromptEditor;
  root.querySelector('#record').onclick = toggleRecording;
  root.querySelector('#organize').onclick = () => showDraft(root);
}

async function toggleRecording() {
  const button = document.querySelector('#record');
  if (state.recording && state.mediaRecorder) {
    state.mediaRecorder.stop(); state.recordingStream?.getTracks().forEach(t => t.stop()); state.recording = false; clearInterval(state.recordTimer); button.classList.remove('recording'); document.querySelector('#record-state').textContent = '录音已完成，可以回放'; return;
  }
  if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) return notify('当前浏览器不支持录音，请使用新版 Chrome 或 Edge');
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    state.recordingStream = stream; state.audioChunks = []; state.mediaRecorder = new MediaRecorder(stream);
    state.mediaRecorder.ondataavailable = e => { if (e.data.size) state.audioChunks.push(e.data); };
    state.mediaRecorder.onstop = () => { if (state.recordedAudioURL) URL.revokeObjectURL(state.recordedAudioURL); state.recordedAudioURL = URL.createObjectURL(new Blob(state.audioChunks, { type: state.mediaRecorder.mimeType || 'audio/webm' })); const result = document.querySelector('#recording-result'); if (result) result.innerHTML = `<audio controls src="${state.recordedAudioURL}"></audio>`; notify('录音已保存，可以立即回放'); };
    state.mediaRecorder.start(); state.recording = true; state.recordingStarted = Date.now(); button.classList.add('recording'); document.querySelector('#record-state').textContent = '正在录音，点击圆点结束'; state.recordTimer = setInterval(() => { const elapsed = Math.floor((Date.now() - state.recordingStarted) / 1000); const el = document.querySelector('#record-time'); if (el) el.textContent = `${String(Math.floor(elapsed / 60)).padStart(2, '0')}:${String(elapsed % 60).padStart(2, '0')}`; }, 500);
  } catch { notify('未获得麦克风权限，可继续使用文字采集'); }
}

function showDraft(root) {
  const text = root.querySelector('#transcript').value.trim(); if (!text) return notify('请先输入一段口述内容');
  const narrator = root.querySelector('#narrator').value.trim() || '家人';
  dialogContent.innerHTML = `<form class="dialog-inner" id="draft-form"><div class="dialog-head"><div><h2>校订记忆草稿</h2><p>AI 负责整理，家人负责确认。</p></div><button type="button" class="icon-button" id="close-dialog">×</button></div><div class="review-note">保存后会标记为“待校订”。你可以在记忆库中继续编辑和确认。</div><div class="form-grid"><div class="form-field span-2"><label>标题</label><input name="title" required value="${esc(narrator)}的一个家传故事"></div><div class="form-field"><label>讲述人</label><input name="author" required value="${esc(narrator)}"></div><div class="form-field"><label>时间线索</label><input name="time" value="待补充"></div><div class="form-field span-2"><label>摘要</label><input name="summary" value="一段由家人亲口讲述、等待共同确认的记忆。"></div><div class="form-field span-2"><label>正文</label><textarea name="content" required>${esc(text)}</textarea></div></div><div class="dialog-actions"><button type="button" class="secondary" id="cancel">取消</button><button class="primary">确认并保存</button></div></form>`;
  dialog.showModal(); dialogContent.querySelector('#close-dialog').onclick = closeDialog; dialogContent.querySelector('#cancel').onclick = closeDialog;
  dialogContent.querySelector('#draft-form').onsubmit = e => { e.preventDefault(); const d = Object.fromEntries(new FormData(e.currentTarget).entries()); state.memories.unshift({ id: `m${Date.now()}`, ...d, place: '', quote: '', tags: ['新记忆'], status: 'needs-review', audio: Boolean(state.recordedAudioURL) }); state.captureText = ''; persist(); closeDialog(); state.tab = 'library'; render(); notify('记忆已保存，等待家人校订'); };
}

function showPromptEditor() {
  dialogContent.innerHTML = `<form class="dialog-inner" id="prompt-form"><div class="dialog-head"><div><h2>编辑问题库</h2><p>每行一个问题，保存后会用于下一次采访。</p></div><button type="button" class="icon-button" id="close-dialog">×</button></div><textarea name="prompts" rows="8">${esc(state.prompts.join('\n'))}</textarea><div class="dialog-actions"><button type="button" class="secondary" id="cancel">取消</button><button class="primary">保存问题</button></div></form>`;
  dialog.showModal(); dialogContent.querySelector('#close-dialog').onclick = closeDialog; dialogContent.querySelector('#cancel').onclick = closeDialog;
  dialogContent.querySelector('#prompt-form').onsubmit = e => { e.preventDefault(); state.prompts = new FormData(e.currentTarget).get('prompts').split('\n').map(x => x.trim()).filter(Boolean).slice(0, 12); persist(); closeDialog(); render(); notify('采访问题库已更新'); };
}

function renderAsk(root) {
  root.innerHTML = `${pageIntro('仅依据家庭资料', '问家', '先从已经保存的记忆里找答案；没有证据时，家声会明确告诉你资料不足。')}<div class="ask-box"><div class="mode-banner">当前为本地检索体验：答案来自浏览器中的演示数据。接入自有 HTTPS 后端后，可替换为带来源的模型问答。</div><label class="hint" for="question">你的问题</label><textarea class="ask-input" id="question" placeholder="例如：外婆做红烧肉最关键的一步是什么？">${esc(state.askQuestion || '')}</textarea><div class="suggestions"><button class="chip" data-question="外婆做红烧肉最关键的一步是什么？">红烧肉的关键步骤</button><button class="chip" data-question="家里有哪些代代相传的手艺？">代代相传的手艺</button><button class="chip" data-question="爷爷第一次远行去了哪里？">爷爷的第一次远行</button></div><button class="primary" id="ask-button">⌕ 从家庭记忆中查找</button><div id="answer"></div></div>`;
  root.querySelectorAll('[data-question]').forEach(b => b.onclick = () => { root.querySelector('#question').value = b.dataset.question; });
  root.querySelector('#ask-button').onclick = () => askQuestion(root);
}
function askQuestion(root) {
  const q = root.querySelector('#question').value.trim(); if (!q) return notify('先写下你想问家人的问题'); state.askQuestion = q;
  const normalized = q.toLocaleLowerCase('zh-CN').replace(/[\s，。？?！!、]/g, '');
  const bigrams = Array.from({ length: Math.max(0, normalized.length - 1) }, (_, i) => normalized.slice(i, i + 2)).filter(x => !['什么', '哪些', '怎么', '如何', '家里', '一个'].includes(x));
  const strongTerms = normalized.match(/红烧肉|糖色|香囊|桂花|收音机|木工尺|缝纫机|全家福|火车|远行|手艺|旧物/g) || [];
  const scored = state.memories.map(m => {
    const haystack = `${m.title}${m.author}${m.content}${m.quote}${m.tags.join('')}`.toLocaleLowerCase('zh-CN');
    const person = personByName(m.author);
    const identityScore = normalized.includes(m.author.toLocaleLowerCase('zh-CN')) || (person?.relation && normalized.includes(person.relation)) ? 4 : 0;
    const tagScore = m.tags.reduce((score, tag) => score + (normalized.includes(tag) ? 3 : 0), 0);
    const textScore = new Set(bigrams.filter(term => haystack.includes(term))).size;
    return { m, score: identityScore + tagScore + textScore };
  }).filter(x => x.score > 0).sort((a, b) => b.score - a.score);
  const targeted = strongTerms.length ? scored.filter(({m}) => strongTerms.some(term => `${m.title}${m.content}${m.quote}${m.tags.join('')}`.toLocaleLowerCase('zh-CN').includes(term))) : scored;
  const pool = targeted.length ? targeted : scored;
  const topScore = pool[0]?.score || 0;
  const relevanceFloor = Math.max(3, topScore * (topScore >= 8 ? .75 : .55));
  const ranked = pool.filter(x => x.score >= relevanceFloor).slice(0, 3);
  const answer = root.querySelector('#answer');
  if (!ranked.length) { answer.innerHTML = `<div class="answer-block"><div class="answer-status">资料不足</div><p class="answer-copy">家庭记忆中暂未找到相关记录。家声不会根据常识猜测答案。</p><button class="secondary" id="capture-from-ask">去采集一段新记忆</button></div>`; answer.querySelector('#capture-from-ask').onclick = () => { state.tab = 'capture'; render(); }; return; }
  const primary = ranked[0].m; const keyLine = primary.quote || primary.summary; answer.innerHTML = `<div class="answer-block"><div class="answer-status">✓ 找到 ${ranked.length} 条相关家庭记忆</div><p class="answer-copy">${esc(keyLine)}${primary.content ? ` ${esc(primary.content.split(/[。！？]/)[0])}。` : ''}</p><div class="section-head"><h2>来源证据</h2><span>可回到原始记忆</span></div><div class="source-list">${ranked.map(({m}) => `<button class="source" data-memory="${m.id}"><strong>${esc(m.title)}</strong><small>${esc(m.author)} · ${esc(m.time)} · ${statusLabel(m.status)}<br>“${esc(m.quote || m.summary)}”</small></button>`).join('')}</div></div>`;
  answer.querySelectorAll('[data-memory]').forEach(b => b.onclick = () => showMemory(b.dataset.memory));
}

function renderFamily(root) {
  root.innerHTML = `${pageIntro('成员与数据', '家庭协作', '让每个人知道自己能做什么，也让每段声音都保留清晰的授权边界。', '<button class="primary" id="add-person">＋ 添加成员</button>')}<div class="two-column"><section><div class="section-head"><h2>家庭成员</h2><span>${state.people.length} 位成员 · 可编辑</span></div><div class="member-grid">${state.people.map(p => `<button class="member-card" data-person="${p.id}"><div class="member-head"><div class="avatar">${esc(p.name.slice(-1))}</div><div><strong>${esc(p.name)}</strong><small>${esc(p.relation)} · ${esc(p.role || '家人')}</small></div></div><p class="member-bio">${esc(p.bio || '还没有写下这位家人的介绍。')}</p></button>`).join('') || '<div class="empty">还没有家庭成员，先添加一位讲述人吧。</div>'}</div></section><aside class="side-guide"><section class="panel"><div class="panel-title"><h2>家庭资料</h2><span>本机演示</span></div><div class="data-list"><div class="data-row"><strong>家庭记忆</strong><span>${state.memories.length} 条</span></div><div class="data-row"><strong>待校订</strong><span>${state.memories.filter(m => m.status !== 'confirmed').length} 条</span></div><div class="data-row"><strong>原声记录</strong><span>${state.memories.filter(m => m.audio).length} 条</span></div><div class="data-row"><strong>保存位置</strong><span>当前浏览器</span></div></div></section><section class="panel"><div class="panel-title"><h2>声音与 AI</h2></div><div class="data-list"><div class="data-row"><strong>系统朗读</strong><span style="color:var(--sage)">可用</span></div><div class="data-row"><strong>声音复刻</strong><span>未启用</span></div></div><p class="hint">音色复刻必须由声音本人单独授权，并提供试听、撤回和删除入口。当前体验只使用系统朗读。</p></section><section class="panel"><div class="panel-title"><h2>隐私承诺</h2></div><div class="privacy-note">记忆正文默认保存在本机。使用 AI 整理或问答时，仅发送完成该请求所需的文字；不会在未经明确同意时克隆家人音色。</div><button class="danger" id="delete-all">删除本机全部数据</button></section></aside></div>`;
  root.querySelector('#add-person').onclick = () => showPersonForm(); root.querySelectorAll('[data-person]').forEach(b => b.onclick = () => showPersonForm(b.dataset.person));
  root.querySelector('#delete-all').onclick = () => { if (confirm('确定删除当前浏览器中的全部家庭资料？')) { state.memories = []; state.people = []; persist(); render(); notify('家庭资料已删除'); } };
}

function showPersonForm(id = '') {
  const person = state.people.find(p => p.id === id);
  dialogContent.innerHTML = `<form class="dialog-inner" id="person-form"><div class="dialog-head"><div><h2>${person ? '编辑家庭成员' : '添加家庭成员'}</h2><p>角色只用于说明协作权限，不会改变记忆内容。</p></div><button type="button" class="icon-button" id="close-dialog">×</button></div><div class="form-grid"><div class="form-field"><label for="person-name">姓名</label><input id="person-name" name="name" required maxlength="30" value="${esc(person?.name || '')}" placeholder="例如：林秀兰"></div><div class="form-field"><label for="person-relation">家庭关系</label><input id="person-relation" name="relation" value="${esc(person?.relation || '')}" placeholder="例如：外婆"></div><div class="form-field"><label for="person-role">角色</label><select id="person-role" name="role"><option ${person?.role === '讲述者' ? 'selected' : ''}>讲述者</option><option ${person?.role === '编辑者' ? 'selected' : ''}>编辑者</option><option ${person?.role === '只读成员' ? 'selected' : ''}>只读成员</option></select></div><div class="form-field"><label for="person-visibility">可见范围</label><select id="person-visibility" name="visibility"><option>家庭空间</option><option>仅自己</option></select></div><div class="form-field span-2"><label for="person-bio">人物介绍</label><textarea id="person-bio" name="bio" rows="3" placeholder="擅长什么、经历过什么，帮助下一代找到采访入口。">${esc(person?.bio || '')}</textarea></div></div><div class="dialog-actions">${person ? '<button type="button" class="danger" id="delete-person">删除成员</button>' : ''}<button type="button" class="secondary" id="cancel">取消</button><button class="primary">保存成员</button></div></form>`;
  dialog.showModal(); dialogContent.querySelector('#close-dialog').onclick = closeDialog; dialogContent.querySelector('#cancel').onclick = closeDialog;
  if (person) dialogContent.querySelector('#delete-person').onclick = () => { if (confirm('删除成员不会删除已有记忆，确定继续？')) { state.people = state.people.filter(p => p.id !== person.id); persist(); closeDialog(); render(); notify('成员已删除'); } };
  dialogContent.querySelector('#person-form').onsubmit = e => { e.preventDefault(); const data = Object.fromEntries(new FormData(e.currentTarget).entries()); const next = { id: person?.id || `p${Date.now()}`, ...data }; if (person) Object.assign(person, next); else state.people.push(next); persist(); closeDialog(); render(); notify(person ? '成员信息已更新' : '家庭成员已添加'); };
}

function exportData() {
  const blob = new Blob([JSON.stringify({ exportedAt: new Date().toISOString(), memories: state.memories, people: state.people }, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = `kinvoice-family-${new Date().toISOString().slice(0, 10)}.json`; link.click(); URL.revokeObjectURL(url); notify('家庭资料已导出为 JSON');
}
function resetData() { if (!confirm('恢复演示数据会覆盖当前浏览器中的资料，确定继续？')) return; localStorage.removeItem(STORAGE_KEY); Object.assign(state, loadState()); state.tab = 'library'; state.filter = '全部'; state.query = ''; render(); notify('已恢复演示数据'); }
dialog.addEventListener('click', e => { if (e.target === dialog) closeDialog(); });
render();
