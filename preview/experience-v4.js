const interviewVoices = {
  warm: { name: '温和陪伴', detail: '慢一点，适合长辈', rate: .88, pitch: 1.03 },
  steady: { name: '沉稳记录', detail: '低缓、克制', rate: .82, pitch: .92 },
  clear: { name: '清晰引导', detail: '节奏清楚', rate: .96, pitch: 1 }
};
const safeQuestions = [
  '这段故事最早发生在什么时候、什么地方？',
  '当时和你在一起的还有谁？',
  '你还记得事情是怎样一步一步发生的吗？',
  '这件事后来对家里产生了什么影响？',
  '其中最想让家人记住的细节是什么？',
  '这份经验后来传给了谁？',
  '还有什么重要细节，是刚才没有问到的？'
];
const kinVoiceApiBase = `${location.protocol}//${location.hostname}:8000`;
const localAiAvailable = ['127.0.0.1', 'localhost'].includes(location.hostname);

async function callKinVoice(path, payload) {
  const response = await fetch(`${kinVoiceApiBase}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!response.ok) throw new Error(`KinVoice API ${response.status}`);
  return response.json();
}

function interviewPayload(interview) {
  return {
    narrator_name: interview.narrator,
    relation: interview.relation,
    theme: interview.theme,
    turns: interview.turns.map(({ role, content }) => ({ role, content }))
  };
}

function interviewSpeak(text, voiceId = 'warm') {
  if (!window.speechSynthesis) return notify('当前浏览器不支持语音播报');
  speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  const voice = interviewVoices[voiceId] || interviewVoices.warm;
  utterance.lang = 'zh-CN'; utterance.rate = voice.rate; utterance.pitch = voice.pitch;
  speechSynthesis.speak(utterance);
}

window.renderKinVoiceOnboarding = function () {
  const pages = [
    ['声', '每个家庭，都有值得留下的声音', '从一道菜、一门手艺或一次远行开始，把口述变成可以寻找的家庭知识。'],
    ['问', 'AI 负责提问，事实由你确认', '采访者会温和追问并整理草稿；保存前，你可以逐字修改或全部放弃。'],
    ['锁', '默认保存在本机', '只有主动使用 AI 时才发送本次所需文字，资料可以随时导出和删除。']
  ];
  state.onboardingPage ??= 0;
  const page = Math.min(state.onboardingPage, 2);
  app.innerHTML = `<main class="onboarding-shell"><div class="onboarding-progress">${pages.map((_, index) => `<i class="${index <= page ? 'active' : ''}"></i>`).join('')}</div><section class="onboarding-content"><div class="onboarding-mark">${pages[page][0]}</div><h1>${pages[page][1]}</h1><p>${pages[page][2]}</p></section>${page < 2 ? `<div class="onboarding-actions"><button class="primary" id="guide-next">继续</button>${page ? '<button class="ghost" id="guide-back">返回</button>' : ''}</div>` : `<form class="onboarding-form" id="family-start"><h2>先建立第一位家人</h2><div class="form-grid"><div class="form-field"><label for="first-name">姓名或称呼</label><input id="first-name" name="name" required placeholder="例如：妈妈"></div><div class="form-field"><label for="first-relation">与我的关系</label><input id="first-relation" name="relation" placeholder="例如：妈妈"></div></div><button class="primary">创建并开始采访</button><button type="button" class="ghost" id="guide-back">返回</button></form>`}</main>`;
  document.querySelector('#guide-next')?.addEventListener('click', () => { state.onboardingPage++; window.renderKinVoiceOnboarding(); });
  document.querySelector('#guide-back')?.addEventListener('click', () => { state.onboardingPage--; window.renderKinVoiceOnboarding(); });
  document.querySelector('#family-start')?.addEventListener('submit', event => {
    event.preventDefault(); const values = Object.fromEntries(new FormData(event.currentTarget).entries());
    state.people = [{ id: `p${Date.now()}`, name: values.name.trim(), relation: values.relation.trim() || '家人', role: '讲述者', bio: '' }];
    state.onboarded = true; state.tab = 'capture'; persist(); render();
  });
};

function freshInterview() {
  const person = state.people[0];
  return { narrator: person?.name || '', relation: person?.relation || '家人', theme: '童年与家庭', voice: 'warm', shareWithAi: false, started: false, turns: [], question: '', answer: '', finishSuggested: false };
}

window.renderKinVoiceInterview = function (root) {
  if (!state.people.length) {
    root.innerHTML = `${pageIntro('AI 引导式采访', '先添加一位家人', '采访需要明确讲述者，资料才不会混在一起。')}<div class="empty empty-action"><strong>还没有家庭成员</strong><small>先建立一位讲述者，再开始采访。</small><button class="primary" id="add-first-person">添加家人</button></div>`;
    root.querySelector('#add-first-person').onclick = () => showPersonForm(); return;
  }
  state.interview ||= freshInterview();
  if (!state.interview.started) renderInterviewSetup(root); else renderInterviewChat(root);
};

function renderInterviewSetup(root) {
  const interview = state.interview;
  root.innerHTML = `${pageIntro('家庭口述采访', '选择一位 AI 采访者', '每次只问一个问题，结束后由你确认人物简介与记忆。')}<div class="interview-layout"><section class="panel"><div class="form-field"><label>讲述者</label><div class="choice-row">${state.people.map(person => `<button class="chip ${interview.narrator === person.name ? 'active' : ''}" data-narrator="${esc(person.name)}">${esc(person.name)}</button>`).join('')}</div></div><div class="form-field"><label for="interview-theme">采访主题</label><input id="interview-theme" value="${esc(interview.theme)}" placeholder="例如：童年、家常菜、第一次远行"></div><div class="form-field"><label>采访者音色</label><div class="voice-grid">${Object.entries(interviewVoices).map(([id, voice]) => `<button class="voice-card ${interview.voice === id ? 'active' : ''}" data-voice="${id}"><span class="voice-avatar">${voice.name[0]}</span><strong>${voice.name}</strong><small>AI 合成音色 · ${voice.detail}</small></button>`).join('')}</div></div><label class="ai-consent ${localAiAvailable ? '' : 'disabled'}"><input type="checkbox" id="ai-consent" ${interview.shareWithAi ? 'checked' : ''} ${localAiAvailable ? '' : 'disabled'}><span><strong>${localAiAvailable ? '启用真实 AI 动态采访' : '手机体验暂使用本地采访提纲'}</strong><small>${localAiAvailable ? '我同意将本次讲述者称呼、关系、主题和对话文字经 KinVoice 本机后端发送到 cctq.ai 处理。录音不会发送。' : '为保护密钥，本机 AI 后端没有开放到局域网；其余采访与保存流程均可体验。'}</small></span></label><button class="primary wide" id="start-interview">开始采访</button></section><aside class="panel trust-panel"><h2>事实由家人确认</h2><p>AI 只根据本次回答继续追问，不进行心理、健康、政治或财务推断。总结不会自动保存。</p><div class="privacy-note">不勾选授权时，全部文字留在当前浏览器并使用本地安全提纲。</div></aside></div>`;
  root.querySelectorAll('[data-narrator]').forEach(button => button.onclick = () => { const person = personByName(button.dataset.narrator); interview.narrator = person.name; interview.relation = person.relation; renderInterviewSetup(root); });
  root.querySelectorAll('[data-voice]').forEach(button => button.onclick = () => { interview.voice = button.dataset.voice; renderInterviewSetup(root); });
  root.querySelector('#interview-theme').oninput = event => interview.theme = event.target.value;
  root.querySelector('#ai-consent').onchange = event => interview.shareWithAi = event.target.checked;
  root.querySelector('#start-interview').onclick = async () => { interview.started = true; interview.turns = []; if (interview.shareWithAi) notify('正在通过 cctq.ai 准备第一个问题'); await nextInterviewQuestion(); persist(); window.renderKinVoiceInterview(root); };
}

function renderInterviewChat(root) {
  const interview = state.interview;
  const answerCount = interview.turns.filter(turn => turn.role === 'user' && !turn.skipped).length;
  root.innerHTML = `${pageIntro('家庭口述采访', `${esc(interview.narrator)}的采访`, '可以文字或语音回答；当前问题、回答和最终草稿都能修改。')}<div class="interview-layout"><section class="chat-panel"><div class="interviewer-bar"><span class="voice-avatar">${interviewVoices[interview.voice].name[0]}</span><div><strong>${interviewVoices[interview.voice].name}</strong><small>AI 合成音色 · 第 ${answerCount + 1} 个问题</small></div><button class="icon-button" id="repeat-question" title="再次播报">▶</button></div><div class="question-editor"><label for="current-question">当前问题（可编辑）</label><textarea id="current-question">${esc(interview.question)}</textarea></div><div class="form-field"><label for="interview-answer">你的回答</label><textarea id="interview-answer" placeholder="在这里回答，也可以点击语音输入">${esc(interview.answer)}</textarea></div><div class="answer-tools"><button class="secondary" id="voice-answer">◉ 语音输入</button><button class="primary" id="submit-answer">回答并继续</button></div><div class="interview-actions"><button class="text-button" id="skip-question">跳过</button><button class="text-button" id="view-transcript">查看完整对话</button><button class="text-button" id="finish-interview" ${answerCount ? '' : 'disabled'}>结束并整理</button></div><button class="ghost danger-text" id="abandon-interview">放弃本次采访</button></section><aside class="panel"><div class="panel-title"><h2>采访进度</h2><span>${answerCount} 段回答</span></div><div class="step-list"><div class="step done"><span class="step-mark">1</span><div><strong>选择讲述者与主题</strong><small>${esc(interview.theme)}</small></div></div><div class="step active"><span class="step-mark">2</span><div><strong>逐轮讲述</strong><small>通常 3–8 个问题即可形成一组记忆。</small></div></div><div class="step"><span class="step-mark">3</span><div><strong>校订并保存</strong><small>人物简介和每条记忆都由你决定。</small></div></div></div></aside></div>`;
  root.querySelector('#repeat-question').onclick = () => interviewSpeak(interview.question, interview.voice);
  root.querySelector('#current-question').oninput = event => { interview.question = event.target.value; const last = interview.turns.at(-1); if (last?.role === 'assistant') last.content = interview.question; };
  root.querySelector('#interview-answer').oninput = event => interview.answer = event.target.value;
  root.querySelector('#voice-answer').onclick = () => interviewSpeechInput(root);
  root.querySelector('#submit-answer').onclick = async () => { if (!interview.answer.trim()) return notify('请先回答，或选择跳过'); interview.turns.push({ role: 'user', content: interview.answer.trim() }); interview.answer = ''; if (interview.shareWithAi) notify('AI 正在根据回答继续追问'); await nextInterviewQuestion(); persist(); window.renderKinVoiceInterview(root); };
  root.querySelector('#skip-question').onclick = async () => { interview.turns.push({ role: 'user', content: '[SKIPPED]', skipped: true }); await nextInterviewQuestion(); persist(); window.renderKinVoiceInterview(root); };
  root.querySelector('#view-transcript').onclick = showInterviewTranscript;
  root.querySelector('#finish-interview').onclick = async () => { if (interview.shareWithAi) notify('AI 正在整理人物简介与记忆'); await showInterviewSummary(); };
  root.querySelector('#abandon-interview').onclick = () => { if (confirm('本次对话尚未保存，确定放弃？')) { state.interview = freshInterview(); persist(); render(); } };
}

async function nextInterviewQuestion() {
  const interview = state.interview;
  const answers = interview.turns.filter(turn => turn.role === 'user' && !turn.skipped).length;
  let question = answers ? safeQuestions[Math.min(answers - 1, safeQuestions.length - 1)] : `${interview.narrator}，关于${interview.theme}，你最想先从哪件事讲起？`;
  if (interview.shareWithAi) {
    try {
      const result = await callKinVoice('/v1/interviews/next', interviewPayload(interview));
      question = result.question; interview.finishSuggested = result.should_finish; interview.online = !result.degraded;
    } catch { interview.online = false; notify('AI 服务暂不可用，已切换本地采访提纲'); }
  } else interview.online = false;
  interview.question = question; interview.turns.push({ role: 'assistant', content: question });
  setTimeout(() => interviewSpeak(question, interview.voice), 100);
}

function interviewSpeechInput(root) {
  const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!Recognition) return notify('当前浏览器不支持语音识别，请使用新版 Chrome 或 Edge');
  const recognition = new Recognition(); recognition.lang = 'zh-CN';
  recognition.onresult = event => { state.interview.answer = event.results[0][0].transcript; root.querySelector('#interview-answer').value = state.interview.answer; notify('语音已转成文字，请确认后发送'); };
  recognition.onerror = () => notify('没有识别到语音，可继续手动输入'); recognition.start(); notify('正在听，请开始讲述');
}

function showInterviewTranscript() {
  const interview = state.interview;
  dialogContent.innerHTML = `<div class="dialog-inner"><div class="dialog-head"><div><h2>完整对话</h2><p>删除回答后，最终总结不会使用该段内容。</p></div><button class="icon-button" id="close-dialog">×</button></div><div class="transcript-list">${interview.turns.filter(turn => !turn.skipped).map(turn => `<div class="transcript-item ${turn.role}"><strong>${turn.role === 'assistant' ? 'AI' : esc(interview.narrator)}</strong><p>${esc(turn.content)}</p>${turn.role === 'user' ? `<button class="text-button" data-delete-answer="${interview.turns.indexOf(turn)}">删除回答</button>` : ''}</div>`).join('')}</div><div class="dialog-actions"><button class="primary" id="done">完成</button></div></div>`;
  dialog.showModal(); dialogContent.querySelector('#close-dialog').onclick = dialogContent.querySelector('#done').onclick = closeDialog;
  dialogContent.querySelectorAll('[data-delete-answer]').forEach(button => button.onclick = () => { interview.turns.splice(Number(button.dataset.deleteAnswer), 1); persist(); showInterviewTranscript(); });
}

function localInterviewSummary() {
  const interview = state.interview;
  const answers = interview.turns.filter(turn => turn.role === 'user' && !turn.skipped && turn.content.trim());
  return answers.slice(0, 5).map((turn, index) => { const first = turn.content.split(/[。！？!?\n]/)[0].trim(); return { index, title: first.slice(0, 28) || `${interview.theme}的一段讲述`, content: turn.content, summary: turn.content.slice(0, 100), quote: first.slice(0, 100), tags: [interview.theme] }; });
}

async function showInterviewSummary() {
  const interview = state.interview; const memories = localInterviewSummary();
  if (!memories.length) return notify('至少回答一个问题后才能整理');
  let bio = `${interview.narrator}在本次采访中讲述了“${interview.theme}”相关经历，共留下 ${memories.length} 段口述。`; let drafts = memories;
  if (interview.shareWithAi) {
    try {
      const result = await callKinVoice('/v1/interviews/summarize', interviewPayload(interview));
      bio = result.profile.bio || bio;
      drafts = result.memories.map((memory, index) => ({ index, title: memory.title, content: memory.content, summary: memory.summary, quote: memory.quote || '', tags: memory.topics || [interview.theme], time: memory.time_hint || '', place: memory.location || '' }));
      interview.online = !result.degraded;
    } catch { interview.online = false; notify('AI 总结暂不可用，已生成本地可编辑草稿'); }
  }
  dialogContent.innerHTML = `<form class="dialog-inner" id="summary-form"><div class="dialog-head"><div><h2>校订采访结果</h2><p>${interview.online ? 'cctq.ai 已根据本次回答整理' : '已使用本地安全整理'}，未确认前不会写入记忆库。</p></div><button type="button" class="icon-button" id="close-dialog">×</button></div><div class="review-note">请修改不准确内容，并取消不想保存的记忆。</div><div class="form-field"><label>人物简介</label><textarea name="bio">${esc(bio)}</textarea></div><div class="summary-drafts">${drafts.map(memory => `<section class="summary-draft"><label class="check-row"><input type="checkbox" name="selected-${memory.index}" checked> 保存为记忆</label><div class="form-field"><label>标题</label><input name="title-${memory.index}" value="${esc(memory.title)}" required></div><div class="form-field"><label>正文</label><textarea name="content-${memory.index}" required>${esc(memory.content)}</textarea></div></section>`).join('')}</div><div class="dialog-actions"><button type="button" class="secondary" id="cancel">返回采访</button><button class="primary">确认并保存</button></div></form>`;
  dialog.showModal(); dialogContent.querySelector('#close-dialog').onclick = dialogContent.querySelector('#cancel').onclick = closeDialog;
  dialogContent.querySelector('#summary-form').onsubmit = event => {
    event.preventDefault(); const form = new FormData(event.currentTarget); const selected = drafts.filter(memory => form.get(`selected-${memory.index}`)); if (!selected.length) return notify('请至少选择一条记忆');
    const person = personByName(interview.narrator); if (person) person.bio = String(form.get('bio')).trim();
    selected.forEach(memory => state.memories.unshift({ id: `m${Date.now()}-${memory.index}`, title: String(form.get(`title-${memory.index}`)).trim(), content: String(form.get(`content-${memory.index}`)).trim(), summary: memory.summary, quote: memory.quote, tags: memory.tags, author: interview.narrator, time: memory.time || '', place: memory.place || '', status: 'needs-review', audio: false }));
    state.interview = freshInterview(); state.tab = 'library'; persist(); closeDialog(); render(); notify(`已保存 ${selected.length} 条待校订记忆`);
  };
}

render();
