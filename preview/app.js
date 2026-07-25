const state = {
  tab: 'library',
  recording: false,
  libraryQuery: '',
  mediaRecorder: null,
  recordingStream: null,
  audioChunks: [],
  recordedAudioURL: '',
  memories: [
    { id: 'm1', title: '外婆的红烧肉：先炒糖色，再等香气', author: '林秀兰', summary: '一道从曾外婆传下来的年夜饭做法。', time: '1978 年除夕', place: '苏州', quote: '糖色不能急，闻到焦糖香才算到时候。', content: '外婆说，糖色不能急。冰糖在小火里慢慢化开，颜色像老茶时才下肉。肉要先擦干水，不然油会溅。加热水没过一半，最后十分钟再放盐。\n\n她年轻时第一次掌勺是在 1978 年的除夕，这道菜后来成了家里的年夜饭。', tags: ['家常菜','年夜饭'] },
    { id: 'm2', title: '旧缝纫机旁学会的耐心', author: '周明芳', summary: '母亲教女儿做衣服，也教她遇事先量清楚。', time: '1980 年代', place: '无锡', quote: '剪刀落下去就不能反悔，先量清楚。', content: '妈妈踩缝纫机前，总会把布铺平量三遍。她说剪刀落下去就不能反悔，生活里许多事也是这样。\n\n那台缝纫机是她结婚时买的，后来给家里四个孩子做过棉衣。', tags: ['手艺','家训'] },
    { id: 'm3', title: '爷爷第一次坐火车去上海', author: '陈国安', summary: '一张硬座票和一只搪瓷杯，装着第一次远行的忐忑。', time: '1966 年夏', place: '上海', quote: '车窗外的电线杆一根接一根，像在带我往前走。', content: '爷爷十八岁第一次坐火车去上海学修机器。奶奶给他装了三个饭团，他一路舍不得吃完。到了上海以后，他把那张硬座票夹进字典里，提醒自己学会一门真正能帮到别人的手艺。', tags: ['远行','成长'] },
    { id: 'm4', title: '端午香囊里的草木配方', author: '林秀兰', summary: '外婆每年端午都会教孩子辨认艾叶、薄荷和藿香。', time: '每年端午', place: '苏州', quote: '先闻，再认叶子的边，草木才记得牢。', content: '外婆会把晒干的艾叶、薄荷和藿香分成小碟，让孩子蒙着眼睛闻。认对以后，再一起装进布袋缝好。她说配方不是最重要的，记住季节和一家人一起做香囊的过程才重要。', tags: ['节气','手艺'] },
    { id: 'm5', title: '爸爸教我的第一张全家福', author: '陈建平', summary: '不是看镜头，而是先让每个人都放松下来。', time: '2003 年春节', place: '杭州', quote: '好照片不是把人排整齐，是把那一刻留下来。', content: '爸爸把胶片相机交给我时，只讲了光圈和快门最基本的用法。他更在意我有没有看到每个人当时的样子。那张全家福里，小姨正在笑，外公还没来得及看镜头，后来反而成了大家最喜欢的一张。', tags: ['影像','家庭'] }
  ],
  people: [{id:'p1', name:'林秀兰', relation:'外婆'}, {id:'p2', name:'周明芳', relation:'妈妈'}, {id:'p3', name:'陈国安', relation:'爷爷'}]
};

const app = document.querySelector('#app-content');
const dialog = document.querySelector('#dialog');
const dialogContent = document.querySelector('#dialog-content');
const toast = document.querySelector('#toast');

document.querySelectorAll('.tabbar button').forEach(button => button.addEventListener('click', () => {
  state.tab = button.dataset.tab;
  document.querySelectorAll('.tabbar button').forEach(x => x.classList.toggle('active', x === button));
  render();
}));

function header(eyebrow, title) { return `<header class="page-header"><div class="eyebrow">${eyebrow}</div><h2>${title}</h2></header>`; }
function esc(value='') { return value.replace(/[&<>"]/g, x => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[x])); }
function notify(text) { toast.textContent = text; toast.classList.add('show'); setTimeout(() => toast.classList.remove('show'), 1800); }
function memoryRows(filter='') {
  const normalized = filter.trim().toLocaleLowerCase('zh-CN');
  const matches = state.memories.filter(m => `${m.title}${m.author}${m.summary}${m.content}${m.tags.join('')}`.toLocaleLowerCase('zh-CN').includes(normalized));
  return {
    count: matches.length,
    html: matches.map(m => `<button class="memory-row" data-memory="${m.id}"><span class="memory-mark">${esc(m.author.slice(-1))}</span><span><strong>${esc(m.title)}</strong><small>${esc(m.author)} · ${esc(m.summary)}</small></span><span class="chevron">›</span></button>`).join('') || '<div class="empty">没有找到相关记忆<br><small>试试人物、地点、手艺或原话</small></div>'
  };
}

function bindMemoryRows() {
  app.querySelectorAll('[data-memory]').forEach(x => x.addEventListener('click', () => showMemory(x.dataset.memory)));
}

function render() {
  if (state.tab === 'library') renderLibrary();
  if (state.tab === 'capture') renderCapture();
  if (state.tab === 'ask') renderAsk();
  if (state.tab === 'family') renderFamily();
}

function renderLibrary(filter='') {
  state.libraryQuery = filter;
  const rows = memoryRows(filter);
  app.innerHTML = `${header('家庭知识传承库','家声')}<div class="page-body"><p class="intro">把家人的声音、经验和手艺，留给下一代。</p><div class="search"><input id="search" aria-label="搜索家人的记忆" placeholder="搜索人物、地点、手艺或原话" value="${esc(filter)}"></div><div class="section-title"><span>${filter ? '搜索结果' : '最近保存'}</span><span id="memory-count">${rows.count} 条</span></div><div class="memory-list" id="memory-list">${rows.html}</div></div>`;
  app.querySelector('#search').addEventListener('input', e => {
    state.libraryQuery = e.target.value;
    const next = memoryRows(state.libraryQuery);
    app.querySelector('#memory-list').innerHTML = next.html;
    app.querySelector('#memory-count').textContent = `${next.count} 条`;
    bindMemoryRows();
  });
  bindMemoryRows();
}

function showMemory(id) {
  const m = state.memories.find(x => x.id === id); if (!m) return;
  dialogContent.innerHTML = `<div class="dialog-inner"><div class="dialog-head"><div><h3>${esc(m.title)}</h3><div class="meta">${esc([m.author, m.time, m.place].filter(Boolean).join(' · '))}</div></div><button class="icon-button" aria-label="关闭">×</button></div>${m.quote ? `<div class="quote">“${esc(m.quote)}”</div>` : ''}<div class="article">${esc(m.content)}</div><div class="tags">${m.tags.map(x=>'#'+esc(x)).join('  ')}</div>${m.audioURL ? `<audio class="memory-audio" controls src="${m.audioURL}"></audio>` : ''}<div class="dialog-actions"><button class="secondary" id="speak">系统朗读</button><button class="primary" id="close-detail">完成</button></div></div>`;
  dialog.showModal();
  dialogContent.querySelector('.icon-button').onclick = () => dialog.close();
  dialogContent.querySelector('#close-detail').onclick = () => dialog.close();
  dialogContent.querySelector('#speak').onclick = () => { speechSynthesis.cancel(); speechSynthesis.speak(new SpeechSynthesisUtterance(m.content)); notify('正在使用系统语音朗读'); };
}

function renderCapture() {
  app.innerHTML = `${header('引导式采访','采集口述')}<div class="page-body">
    <div class="form-section"><label for="narrator">这次和谁聊</label><input id="narrator" placeholder="例如：外婆" value="外婆"></div>
    <div class="form-section"><label for="chosen-prompt">采访问题（可直接修改）</label><textarea class="question-box" id="chosen-prompt">小时候家里过年，最不能少的一道菜是什么？</textarea><div class="prompt-options"><button>你年轻时学会的第一门手艺是什么？</button><button>这道家常菜是谁教你的？</button><button>家里有哪条规矩，背后藏着什么故事？</button></div></div>
    <div class="form-section"><label>口述记录</label><button class="record" id="record">● 开始录音</button><div id="recording-result">${state.recordedAudioURL ? `<audio class="memory-audio" controls src="${state.recordedAudioURL}"></audio><div class="recording-note">录音只保存在当前浏览器会话</div>` : ''}</div><textarea id="transcript" placeholder="输入或粘贴口述文字；录音与文字会一起保存在这条记忆中。">我第一次做这道红烧肉是七八年除夕。冰糖要用小火慢慢化开，等颜色像老茶、闻到焦糖香再下肉。肉要擦干，加热水，盐最后放。</textarea></div>
    <button class="primary" id="organize">✦ 整理成记忆草稿</button></div>`;
  app.querySelectorAll('.prompt-options button').forEach(x => x.onclick = () => app.querySelector('#chosen-prompt').value = x.textContent);
  app.querySelector('#record').onclick = toggleRecording;
  app.querySelector('#organize').onclick = showDraft;
}

async function toggleRecording(event) {
  const button = event.currentTarget;
  if (state.recording && state.mediaRecorder) {
    state.mediaRecorder.stop();
    state.recordingStream?.getTracks().forEach(track => track.stop());
    state.recording = false;
    button.classList.remove('recording');
    button.textContent = '● 重新录音';
    return;
  }
  if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) return notify('当前浏览器不支持录音，请使用新版 Chrome 或 Edge');
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    state.audioChunks = [];
    state.recordingStream = stream;
    state.mediaRecorder = new MediaRecorder(stream);
    state.mediaRecorder.ondataavailable = e => { if (e.data.size) state.audioChunks.push(e.data); };
    state.mediaRecorder.onstop = () => {
      if (state.recordedAudioURL) URL.revokeObjectURL(state.recordedAudioURL);
      state.recordedAudioURL = URL.createObjectURL(new Blob(state.audioChunks, { type: state.mediaRecorder.mimeType || 'audio/webm' }));
      app.querySelector('#recording-result').innerHTML = `<audio class="memory-audio" controls src="${state.recordedAudioURL}"></audio><div class="recording-note">录音只保存在当前浏览器会话</div>`;
      notify('录音已保存，可以立即回放');
    };
    state.mediaRecorder.start();
    state.recording = true;
    button.classList.add('recording');
    button.textContent = '■ 结束录音';
  } catch {
    notify('未获得麦克风权限，可继续使用文字采集');
  }
}

function showDraft() {
  const text = app.querySelector('#transcript').value.trim();
  if (!text) return notify('请先输入一段口述内容');
  const narrator = app.querySelector('#narrator').value.trim() || '家人';
  dialogContent.innerHTML = `<form class="dialog-inner" id="draft-form"><div class="dialog-head"><h3>校订记忆草稿</h3><button type="button" class="icon-button">×</button></div><div class="review-note">AI 只负责整理。请由家人确认事实和措辞后保存。</div><div class="form-section"><label>标题</label><input name="title" value="外婆的红烧肉：糖色不能急"></div><div class="form-section"><label>摘要</label><input name="summary" value="一道从长辈手里传下来的年夜饭做法。"></div><div class="form-section"><label>正文</label><textarea name="content">${esc(text)}</textarea></div><div class="form-section"><label>时间线索</label><input name="time" value="1978 年除夕"></div><div class="dialog-actions"><button type="button" class="secondary" id="cancel">取消</button><button class="primary">确认并保存</button></div></form>`;
  dialog.showModal();
  dialogContent.querySelector('.icon-button').onclick = () => dialog.close(); dialogContent.querySelector('#cancel').onclick = () => dialog.close();
  dialogContent.querySelector('#draft-form').onsubmit = e => { e.preventDefault(); const f = new FormData(e.currentTarget); state.memories.unshift({id:'m'+Date.now(), title:f.get('title'), author:narrator, summary:f.get('summary'), time:f.get('time'), place:'', quote:'糖色不能急。', content:f.get('content'), tags:['家常菜','待家人确认'], audioURL: state.recordedAudioURL}); dialog.close(); notify('记忆已保存在本机'); state.tab='library'; document.querySelector('[data-tab="library"]').click(); };
}

function renderAsk() {
  app.innerHTML = `${header('仅依据家庭资料','问家')}<div class="page-body"><div class="form-section"><label for="question">你想问什么</label><textarea id="question" placeholder="例如：外婆做红烧肉最关键的一步是什么？">外婆做红烧肉最关键的一步是什么？</textarea></div><button class="primary" id="ask-button">⌕ 从家庭记忆中查找</button><div id="answer"></div></div>`;
  app.querySelector('#ask-button').onclick = () => {
    const q = app.querySelector('#question').value;
    const relevant = /红烧肉|糖色|外婆|年夜饭/.test(q);
    app.querySelector('#answer').innerHTML = relevant ? `<div class="answer"><div class="grounded">✓ 答案来自 1 条家庭记忆</div><p>最关键的是用小火慢慢炒糖色。等冰糖颜色像老茶、闻到焦糖香时再下擦干水的肉；最后十分钟才放盐。</p><div class="section-title">来源</div><button class="source" data-memory="m1"><strong>外婆的红烧肉：先炒糖色，再等香气</strong><small>“糖色不能急，闻到焦糖香才算到时候。”</small></button></div>` : `<div class="answer"><div class="grounded">资料不足</div><p>家庭记忆中暂未找到相关记录。家声不会根据常识猜测答案。</p></div>`;
    app.querySelectorAll('[data-memory]').forEach(x => x.onclick = () => showMemory(x.dataset.memory));
  };
}

function renderFamily() {
  app.innerHTML = `${header('成员与数据','家庭')}<div class="page-body"><div class="section-title"><span>家庭成员</span><button class="text-button" id="add-person">＋ 添加</button></div>${state.people.map(p=>`<button class="member member-button" data-person="${p.id}"><div class="avatar">${esc(p.name.slice(-1))}</div><strong>${esc(p.name)}</strong><span>${esc(p.relation)}　›</span></button>`).join('') || '<div class="empty compact">还没有家庭成员</div>'}<div class="section-title">数据</div><div class="data-row"><strong>家庭记忆</strong><span>${state.memories.length} 条</span></div><div class="data-row"><strong>保存位置</strong><span>本机</span></div><div class="section-title">声音与 AI</div><div class="data-row"><strong>系统朗读</strong><span class="status-ready">可用</span></div><div class="data-row"><strong>家人音色复刻</strong><span>未启用</span></div><p class="meta consent-copy">音色复刻必须由声音本人单独授权，并提供试听、撤回和删除入口。当前参赛版本先使用系统语音朗读。</p><div class="section-title">隐私承诺</div><p class="meta">记忆正文默认保存在本机。使用 AI 整理或问答时，仅发送完成该请求所需的文字；不会在未经明确同意时克隆家人音色。</p><button class="danger" id="delete-all">删除本机全部演示数据</button></div>`;
  app.querySelector('#add-person').onclick = () => showPersonForm();
  app.querySelectorAll('[data-person]').forEach(x => x.onclick = () => showPersonForm(x.dataset.person));
  app.querySelector('#delete-all').onclick = () => { if(confirm('确定删除本次预览中的全部演示数据？刷新页面可恢复。')) { state.memories=[]; state.people=[]; renderFamily(); notify('演示数据已删除'); } };
}

function showPersonForm(id='') {
  const person = state.people.find(p => p.id === id);
  dialogContent.innerHTML = `<form class="dialog-inner" id="person-form"><div class="dialog-head"><h3>${person ? '编辑家庭成员' : '添加家庭成员'}</h3><button type="button" class="icon-button">×</button></div><div class="form-section"><label>姓名</label><input name="name" required maxlength="30" value="${esc(person?.name || '')}" placeholder="例如：林秀兰"></div><div class="form-section"><label>家庭关系</label><input name="relation" maxlength="30" value="${esc(person?.relation || '')}" placeholder="例如：外婆"></div>${person ? '<button type="button" class="danger" id="delete-person">删除成员</button>' : ''}<div class="dialog-actions"><button type="button" class="secondary" id="cancel">取消</button><button class="primary">保存</button></div></form>`;
  dialog.showModal();
  dialogContent.querySelector('.icon-button').onclick = () => dialog.close();
  dialogContent.querySelector('#cancel').onclick = () => dialog.close();
  if (person) dialogContent.querySelector('#delete-person').onclick = () => { state.people = state.people.filter(p => p.id !== person.id); dialog.close(); renderFamily(); notify('成员已删除，已有记忆不会受影响'); };
  dialogContent.querySelector('#person-form').onsubmit = e => {
    e.preventDefault();
    const data = new FormData(e.currentTarget);
    const next = { id: person?.id || `p${Date.now()}`, name: data.get('name').trim(), relation: data.get('relation').trim() || '家人' };
    if (person) Object.assign(person, next); else state.people.push(next);
    dialog.close(); renderFamily(); notify(person ? '成员信息已更新' : '家庭成员已添加');
  };
}

dialog.addEventListener('click', e => { if (e.target === dialog) dialog.close(); });
render();
