const stages = {
  development: '개발 중',
  testing: '테스트 중',
  released: '출시'
};
const outcomes = {
  queued: '대기 중',
  in_progress: '실행 중',
  success: '성공',
  failure: '실패',
  cancelled: '취소됨',
  skipped: '건너뜀'
};
const platforms = { android: 'Android', web: 'Web' };
const artifactNames = { apk: 'APK', aab: 'AAB', web: '웹 빌드', test: '테스트 결과' };
const unconnected = '미연결';
const formatTime = new Intl.DateTimeFormat('ko-KR', {
  dateStyle: 'medium',
  timeStyle: 'short',
  timeZone: 'Asia/Seoul'
});

const summary = document.querySelector('#summary');
const container = document.querySelector('#apps');
const filter = document.querySelector('#status-filter');
const empty = document.querySelector('#empty');
const filterStatus = document.querySelector('#filter-status');
const loading = document.querySelector('#loading');

function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text != null) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function actionLink(text, url) {
  const link = element('a', text);
  link.href = url;
  return link;
}

function metric(list, label, value, className) {
  const field = element('div');
  field.append(element('dt', label), element('dd', value ?? unconnected, value == null ? 'unconnected' : className));
  list.append(field);
}

function appCard(app) {
  const article = element('article', null, 'app');
  const heading = element('div', null, 'app-heading');
  const identity = element('div');
  identity.append(element('h3', app.appName), element('p', app.appId + ' · ' + platforms[app.platform], 'app-id'));
  heading.append(identity, element('span', stages[app.status], 'stage stage-' + app.status));
  article.append(heading);
  if (app.description) article.append(element('p', app.description, 'description'));

  const latest = app.runs[0];
  const details = element('dl', null, 'details');
  metric(details, '현재 버전', app.versionName + (app.versionCode ? ' (' + app.versionCode + ')' : ''));
  metric(details, '최근 빌드', latest ? outcomes[latest.build] + ' · ' + latest.versionName : null, latest?.build);
  metric(details, '최근 테스트', outcomes[latest?.test], latest?.test);
  metric(details, 'Artifact', latest && Object.keys(latest.artifacts).length ? '다운로드 링크 제공' : null);
  metric(details, '마켓 연동', null);
  metric(details, '광고 수익 연동', null);
  metric(details, '다운로드 통계', null);
  metric(details, '오류 통계', null);
  article.append(details);

  if (latest && Object.keys(latest.artifacts).length) {
    const downloads = element('div', null, 'artifacts');
    for (const [kind, url] of Object.entries(latest.artifacts)) {
      downloads.append(actionLink(artifactNames[kind] + ' 다운로드', url));
    }
    article.append(downloads);
  }

  if (app.runs.length) {
    const history = element('details', null, 'history');
    const runs = element('ul');
    for (const run of app.runs) {
      const row = element('li', null, 'run');
      const time = element('time', formatTime.format(new Date(run.updatedAt)) + ' KST');
      time.dateTime = run.updatedAt;
      row.append(actionLink('Actions 실행 · ' + run.versionName, run.url), time,
        element('span', '빌드 ' + outcomes[run.build], run.build));
      if (run.test) row.append(element('span', '테스트 ' + outcomes[run.test], run.test));
      runs.append(row);
    }
    history.append(element('summary', 'Actions 실행 내역'), runs);
    article.append(history);
  }
  return article;
}

function showApps(apps) {
  const visible = filter.value === 'all' ? apps : apps.filter(app => app.status === filter.value);
  container.replaceChildren(...visible.map(appCard));
  empty.hidden = visible.length > 0;
  filterStatus.textContent = '앱 ' + visible.length + '개 표시';
}

async function loadDashboard() {
  const response = await fetch('./data.json', { cache: 'no-store' });
  if (!response.ok) throw new Error('대시보드 데이터를 읽을 수 없습니다.');
  const data = await response.json();
  const counts = [['전체', data.apps.length], ...Object.entries(stages).map(([status, label]) => [
    label, data.apps.filter(app => app.status === status).length
  ])];
  for (const [label, count] of counts) {
    const item = element('li', label);
    item.append(element('strong', String(count)));
    summary.append(item);
  }

  const actions = document.querySelector('#actions-link');
  actions.href = data.actionsUrl;
  actions.hidden = false;
  filter.addEventListener('change', () => showApps(data.apps));
  showApps(data.apps);
  loading.hidden = true;
  summary.hidden = false;
  document.querySelector('#app-section').hidden = false;
}

loadDashboard().catch(() => {
  loading.textContent = '앱 정보를 불러오지 못했습니다. 대시보드를 다시 빌드하거나 새로고침하세요.';
});
