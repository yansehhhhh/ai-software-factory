<template>
  <div class="app-shell">
    <section v-if="!currentUser" class="login-page">
      <div class="login-card">
        <div class="brand-block">
          <div class="logo-mark">票</div>
          <div>
            <p class="eyebrow">Company Vote</p>
            <h1>公司内部投票系统</h1>
          </div>
        </div>
        <p class="login-intro">使用邮箱或工号加密码登录，系统会根据账号角色进入员工端或管理员工作台。</p>
        <label>邮箱 / 工号</label>
        <input v-model="loginForm.account" placeholder="admin@example.com 或 E10001" />
        <label>密码</label>
        <input v-model="loginForm.password" type="password" placeholder="请输入密码" @keyup.enter="login('employee')" />
        <p v-if="loginError" class="field-error">{{ loginError }}</p>
        <div class="login-actions">
          <button class="primary" @click="login('employee')">员工登录</button>
          <button class="secondary" @click="login('admin')">管理员登录</button>
        </div>
        <p class="helper">演示账号：员工 E10001 / 管理员 admin@example.com，任意密码均可进入。</p>
      </div>
    </section>

    <template v-else>
      <header class="topbar">
        <div class="brand-inline">
          <div class="logo-mark small">票</div>
          <div>
            <strong>内部投票系统</strong>
            <span>{{ currentUser.role === 'ADMIN' ? '管理员工作台' : '员工投票中心' }}</span>
          </div>
        </div>
        <div class="user-area">
          <span class="role-badge">{{ currentUser.role === 'ADMIN' ? '管理员' : '员工' }}</span>
          <span>{{ currentUser.name }}</span>
          <button class="ghost" @click="logout">退出</button>
        </div>
      </header>

      <main class="workspace">
        <aside class="sidebar">
          <button v-for="item in navItems" :key="item.key" :class="{ active: activePage === item.key }" @click="activePage = item.key">
            {{ item.label }}
          </button>
        </aside>

        <section class="content">
          <section v-if="activePage === 'votes'" class="page-section">
            <div class="page-heading">
              <div>
                <p class="eyebrow">投票列表</p>
                <h2>所有员工可参与的投票</h2>
                <p>创建后立即开始，截止后所有员工可查看结果。</p>
              </div>
              <div class="tabs">
                <button :class="{ active: voteFilter === 'ALL' }" @click="voteFilter = 'ALL'">全部</button>
                <button :class="{ active: voteFilter === 'ONGOING' }" @click="voteFilter = 'ONGOING'">进行中</button>
                <button :class="{ active: voteFilter === 'ENDED' }" @click="voteFilter = 'ENDED'">已结束</button>
                <button :class="{ active: voteFilter === 'SUBMITTED' }" @click="voteFilter = 'SUBMITTED'">我已提交</button>
              </div>
            </div>
            <div v-if="filteredVotes.length" class="vote-grid">
              <article v-for="vote in filteredVotes" :key="vote.id" class="vote-card">
                <div class="card-topline">
                  <span :class="['status', vote.status === 'ONGOING' ? 'blue' : 'gray']">{{ vote.status === 'ONGOING' ? '进行中' : '已结束' }}</span>
                  <span :class="['status', vote.mode === 'ANONYMOUS' ? 'cyan' : 'amber']">{{ vote.mode === 'ANONYMOUS' ? '匿名投票' : '实名投票' }}</span>
                  <span v-if="hasSubmitted(vote.id)" class="status green">已提交</span>
                </div>
                <h3>{{ vote.title }}</h3>
                <p>{{ vote.description }}</p>
                <div class="meta-row">
                  <span>截止：{{ vote.endTime }}</span>
                  <span>{{ vote.questions.length }} 题</span>
                </div>
                <div class="progress-line"><span :style="{ width: participationRate(vote) + '%' }"></span></div>
                <small>{{ vote.submissionCount }}/{{ employeeCount }} 人参与，参与率 {{ participationRate(vote) }}%</small>
                <div class="card-actions">
                  <button v-if="vote.status === 'ONGOING' && !hasSubmitted(vote.id)" class="primary" @click="openVote(vote.id)">去投票</button>
                  <button v-else-if="vote.status === 'ONGOING'" disabled>已提交，不可修改</button>
                  <button v-else class="secondary" @click="openResult(vote.id)">查看结果</button>
                </div>
              </article>
            </div>
            <div v-else class="empty-state">
              <h3>暂无投票</h3>
              <p>当前筛选条件下没有投票活动，请切换筛选或稍后查看。</p>
            </div>
          </section>

          <section v-if="activePage === 'detail' && selectedVote" class="page-section detail-layout">
            <div class="vote-detail-main">
              <button class="text-link" @click="activePage = 'votes'">← 返回列表</button>
              <div class="page-heading compact">
                <div>
                  <p class="eyebrow">投票详情</p>
                  <h2>{{ selectedVote.title }}</h2>
                  <p>{{ selectedVote.description }}</p>
                </div>
                <span :class="['status', selectedVote.mode === 'ANONYMOUS' ? 'cyan' : 'amber']">{{ selectedVote.mode === 'ANONYMOUS' ? '匿名投票' : '实名投票' }}</span>
              </div>
              <div :class="['notice', selectedVote.mode === 'ANONYMOUS' ? 'info' : 'warning']">
                {{ selectedVote.mode === 'ANONYMOUS' ? '本投票为匿名投票，结果中不会展示投票人与答案的对应关系。' : '本投票为实名投票，管理员可查看你的提交明细。' }}
              </div>
              <div v-for="(question, index) in selectedVote.questions" :key="question.id" class="question-block">
                <div class="question-title">
                  <strong>{{ index + 1 }}. {{ question.title }}</strong>
                  <span v-if="question.required">必答</span>
                </div>
                <div v-if="question.type === 'SINGLE_CHOICE'" class="options-list">
                  <label v-for="option in question.options" :key="option.id" :class="['option-row', answerOf(question.id).selectedOptionId === option.id ? 'checked' : '']">
                    <input v-model="answerOf(question.id).selectedOptionId" type="radio" :name="question.id" :value="option.id" />
                    {{ option.label }}
                  </label>
                </div>
                <div v-else-if="question.type === 'MULTIPLE_CHOICE'" class="options-list">
                  <label v-for="option in question.options" :key="option.id" :class="['option-row', answerOf(question.id).selectedOptionIds.includes(option.id) ? 'checked' : '']">
                    <input v-model="answerOf(question.id).selectedOptionIds" type="checkbox" :value="option.id" />
                    {{ option.label }}
                  </label>
                  <small>最多选择 {{ question.config.maxSelected }} 项</small>
                </div>
                <div v-else-if="question.type === 'RATING'" class="score-row">
                  <button v-for="score in scoreRange(question)" :key="score" :class="{ active: answerOf(question.id).scoreValue === score }" @click="answerOf(question.id).scoreValue = score">{{ score }}</button>
                </div>
                <textarea v-else v-model="answerOf(question.id).textValue" :maxlength="question.config.maxLength || 500" placeholder="请输入你的反馈建议" />
              </div>
              <div v-if="submitError" class="notice danger">{{ submitError }}</div>
              <div class="sticky-submit">
                <p>提交后即锁定，无法修改。</p>
                <button class="primary" @click="confirmSubmit = true">提交投票</button>
              </div>
            </div>
            <aside class="summary-panel">
              <h3>投票规则</h3>
              <ul>
                <li>所有员工均可参与。</li>
                <li>每人每个投票只能提交一次。</li>
                <li>截止时间：{{ selectedVote.endTime }}</li>
                <li>题型：单选 / 多选 / 评分 / 文本反馈。</li>
              </ul>
            </aside>
          </section>

          <section v-if="activePage === 'results' && selectedVote" class="page-section">
            <div class="page-heading">
              <div>
                <p class="eyebrow">图表化统计结果</p>
                <h2>{{ selectedVote.title }}</h2>
                <p>{{ selectedVote.status === 'ENDED' ? '投票已结束，所有员工可查看汇总统计。' : '投票未结束，员工端暂不可查看最终结果。' }}</p>
              </div>
              <button class="secondary" @click="activePage = 'votes'">返回列表</button>
            </div>
            <div class="metric-grid">
              <div class="metric-card"><span>参与人数</span><strong>{{ selectedVote.submissionCount }}</strong></div>
              <div class="metric-card"><span>总员工数</span><strong>{{ employeeCount }}</strong></div>
              <div class="metric-card"><span>参与率</span><strong>{{ participationRate(selectedVote) }}%</strong></div>
              <div class="metric-card"><span>投票模式</span><strong>{{ selectedVote.mode === 'ANONYMOUS' ? '匿名' : '实名' }}</strong></div>
            </div>
            <div class="result-grid">
              <article v-for="question in selectedVote.questions" :key="question.id" class="result-card">
                <h3>{{ question.title }}</h3>
                <template v-if="question.type === 'SINGLE_CHOICE' || question.type === 'MULTIPLE_CHOICE'">
                  <div v-for="stat in choiceStats(question)" :key="stat.label" class="bar-stat">
                    <span>{{ stat.label }}</span>
                    <div class="bar"><i :style="{ width: stat.percent + '%' }"></i></div>
                    <strong>{{ stat.count }} 票 · {{ stat.percent }}%</strong>
                  </div>
                </template>
                <template v-else-if="question.type === 'RATING'">
                  <div class="rating-summary">平均分 <strong>{{ ratingAverage(question) }}</strong></div>
                  <div class="rating-bars">
                    <div v-for="item in ratingStats(question)" :key="item.score">
                      <span>{{ item.score }}分</span>
                      <div :style="{ height: Math.max(18, item.count * 18) + 'px' }"></div>
                      <small>{{ item.count }}</small>
                    </div>
                  </div>
                </template>
                <template v-else>
                  <p class="text-summary">共收到 {{ textFeedbacks(question).length }} 条文本反馈。</p>
                  <ul class="feedback-list">
                    <li v-for="item in textFeedbacks(question).slice(0, 3)" :key="item">{{ item }}</li>
                  </ul>
                  <small v-if="selectedVote.mode === 'ANONYMOUS'">匿名投票不展示投票人与答案映射。</small>
                </template>
              </article>
            </div>
          </section>

          <section v-if="activePage === 'admin'" class="page-section">
            <div class="page-heading">
              <div>
                <p class="eyebrow">管理员工作台</p>
                <h2>创建、管理投票与员工账号</h2>
                <p>仅管理员可创建和管理投票；所有员工默认可参与每个投票。</p>
              </div>
            </div>
            <div class="metric-grid">
              <div class="metric-card"><span>进行中投票</span><strong>{{ votes.filter(v => v.status === 'ONGOING').length }}</strong></div>
              <div class="metric-card"><span>已结束投票</span><strong>{{ votes.filter(v => v.status === 'ENDED').length }}</strong></div>
              <div class="metric-card"><span>员工账号</span><strong>{{ employeeCount }}</strong></div>
              <div class="metric-card"><span>总提交</span><strong>{{ submissions.length }}</strong></div>
            </div>
            <div class="admin-grid">
              <form class="admin-card" @submit.prevent="createVote">
                <h3>创建投票</h3>
                <label>投票标题</label>
                <input v-model="newVote.title" required />
                <label>说明</label>
                <textarea v-model="newVote.description" />
                <label>结束时间</label>
                <input v-model="newVote.endTime" required />
                <div class="inline-fields">
                  <label><input v-model="newVote.mode" type="radio" value="ANONYMOUS" /> 匿名</label>
                  <label><input v-model="newVote.mode" type="radio" value="REAL_NAME" /> 实名</label>
                </div>
                <div class="inline-fields">
                  <label><input v-model="newVote.structureType" type="radio" value="SINGLE_QUESTION" /> 单题</label>
                  <label><input v-model="newVote.structureType" type="radio" value="MULTI_QUESTION" /> 多题</label>
                </div>
                <button class="primary" type="submit">创建投票并立即开始</button>
                <p class="helper">示例会自动生成单选、多选、评分和文本反馈题。</p>
              </form>
              <div class="admin-card">
                <h3>投票管理</h3>
                <div v-for="vote in votes" :key="vote.id" class="manage-row">
                  <div>
                    <strong>{{ vote.title }}</strong>
                    <p>{{ vote.status === 'ONGOING' ? '进行中' : '已结束' }} · {{ vote.mode === 'ANONYMOUS' ? '匿名' : '实名' }} · {{ vote.submissionCount }}/{{ employeeCount }}</p>
                  </div>
                  <button v-if="vote.status === 'ONGOING'" class="danger" @click="endVote(vote.id)">提前结束</button>
                  <button v-else class="secondary" @click="openResult(vote.id)">结果</button>
                </div>
              </div>
              <div class="admin-card import-card">
                <h3>Excel 批量导入员工账号</h3>
                <p>支持字段：工号、姓名、邮箱、初始密码、角色、状态。</p>
                <input type="file" accept=".xlsx,.xls,.csv" @change="handleImport" />
                <div class="import-result">
                  <strong>导入结果</strong>
                  <p>总行数 {{ importResult.total }}，成功 {{ importResult.success }}，失败 {{ importResult.failed }}</p>
                  <ul v-if="importResult.errors.length">
                    <li v-for="error in importResult.errors" :key="error">{{ error }}</li>
                  </ul>
                </div>
              </div>
            </div>
          </section>
        </section>
      </main>

      <div v-if="confirmSubmit" class="modal-backdrop" @click.self="confirmSubmit = false">
        <div class="modal-card">
          <h3>确认提交投票？</h3>
          <p>提交后不可修改，请确认你的答案无误。</p>
          <div class="modal-actions">
            <button class="ghost" @click="confirmSubmit = false">取消</button>
            <button class="primary" @click="submitVote">确认提交</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'

type Role = 'EMPLOYEE' | 'ADMIN'
type VoteMode = 'REAL_NAME' | 'ANONYMOUS'
type VoteStatus = 'ONGOING' | 'ENDED'
type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'RATING' | 'TEXT'

interface User { id: string; employeeNo: string; name: string; email: string; role: Role }
interface Option { id: string; label: string }
interface Question { id: string; type: QuestionType; title: string; required: boolean; options: Option[]; config: Record<string, number> }
interface Vote { id: string; title: string; description: string; mode: VoteMode; structureType: 'SINGLE_QUESTION' | 'MULTI_QUESTION'; status: VoteStatus; endTime: string; questions: Question[]; submissionCount: number }
interface AnswerDraft { questionId: string; selectedOptionId?: string; selectedOptionIds: string[]; scoreValue?: number; textValue?: string }
interface Submission { voteId: string; userId: string; answers: AnswerDraft[]; submittedAt: string }

const employeeCount = ref(420)
const currentUser = ref<User | null>(null)
const activePage = ref('votes')
const voteFilter = ref<'ALL' | 'ONGOING' | 'ENDED' | 'SUBMITTED'>('ALL')
const selectedVoteId = ref('')
const confirmSubmit = ref(false)
const submitError = ref('')
const loginError = ref('')

const loginForm = reactive({ account: 'admin@example.com', password: 'Admin@123456' })
const importResult = reactive({ total: 0, success: 0, failed: 0, errors: [] as string[] })
const newVote = reactive({ title: '新一轮内部意见征集', description: '请选择或反馈你的建议。', mode: 'ANONYMOUS' as VoteMode, structureType: 'MULTI_QUESTION' as 'SINGLE_QUESTION' | 'MULTI_QUESTION', endTime: '2026-05-30 18:00' })

const users: Record<Role, User> = {
  EMPLOYEE: { id: 'u1', employeeNo: 'E10001', name: '张三', email: 'zhangsan@example.com', role: 'EMPLOYEE' },
  ADMIN: { id: 'u2', employeeNo: 'A10001', name: '王管理员', email: 'admin@example.com', role: 'ADMIN' }
}

const votes = ref<Vote[]>([
  {
    id: 'v1', title: '内部办公体验反馈', description: '请反馈近期办公体验，用于后续优化。', mode: 'ANONYMOUS', structureType: 'MULTI_QUESTION', status: 'ONGOING', endTime: '2026-05-15 18:00', submissionCount: 218,
    questions: [
      { id: 'q1', type: 'RATING', title: '你对当前办公环境的满意度评分是多少？', required: true, options: [], config: { minScore: 1, maxScore: 5 } },
      { id: 'q2', type: 'MULTIPLE_CHOICE', title: '你认为优先需要改善哪些方面？', required: true, options: [{ id: 'o1', label: '会议室预约体验' }, { id: 'o2', label: '办公网络稳定性' }, { id: 'o3', label: '工位空间' }, { id: 'o4', label: '茶水间补给' }], config: { minSelected: 1, maxSelected: 3 } },
      { id: 'q3', type: 'TEXT', title: '请补充你的建议。', required: false, options: [], config: { maxLength: 500 } }
    ]
  },
  {
    id: 'v2', title: '2026 年团建地点投票', description: '请选择你最希望参加团建的地点。', mode: 'ANONYMOUS', structureType: 'SINGLE_QUESTION', status: 'ONGOING', endTime: '2026-05-10 18:00', submissionCount: 301,
    questions: [{ id: 'q4', type: 'SINGLE_CHOICE', title: '你更希望团建去哪里？', required: true, options: [{ id: 'o5', label: '海边' }, { id: 'o6', label: '山区' }, { id: 'o7', label: '城市周边' }, { id: 'o8', label: '室内活动中心' }], config: {} }]
  },
  {
    id: 'v3', title: '年度优秀项目评选', description: '实名投票，管理员可查看提交明细。', mode: 'REAL_NAME', structureType: 'SINGLE_QUESTION', status: 'ENDED', endTime: '2026-04-28 18:00', submissionCount: 356,
    questions: [{ id: 'q5', type: 'SINGLE_CHOICE', title: '请选择你心中的年度优秀项目。', required: true, options: [{ id: 'o9', label: '智能客服平台' }, { id: 'o10', label: '数据中台升级' }, { id: 'o11', label: '移动办公优化' }], config: {} }]
  }
])

const submissions = ref<Submission[]>([
  { voteId: 'v2', userId: 'u1', submittedAt: '2026-05-01 10:30', answers: [{ questionId: 'q4', selectedOptionId: 'o5', selectedOptionIds: [] }] },
  { voteId: 'v3', userId: 'u1', submittedAt: '2026-04-20 15:20', answers: [{ questionId: 'q5', selectedOptionId: 'o9', selectedOptionIds: [] }] }
])

const draftAnswers = ref<AnswerDraft[]>([])

const navItems = computed(() => currentUser.value?.role === 'ADMIN'
  ? [{ key: 'admin', label: '管理员工作台' }, { key: 'votes', label: '员工端投票' }, { key: 'results', label: '结果查看' }]
  : [{ key: 'votes', label: '投票列表' }, { key: 'detail', label: '投票详情' }, { key: 'results', label: '结果查看' }]
)

const selectedVote = computed(() => votes.value.find(v => v.id === selectedVoteId.value) || votes.value[0])
const filteredVotes = computed(() => votes.value.filter(v => {
  if (voteFilter.value === 'ALL') return true
  if (voteFilter.value === 'SUBMITTED') return hasSubmitted(v.id)
  return v.status === voteFilter.value
}))

function login(type: 'employee' | 'admin') {
  loginError.value = ''
  if (!loginForm.account || !loginForm.password) {
    loginError.value = '请输入邮箱/工号和密码。'
    return
  }
  currentUser.value = type === 'admin' ? users.ADMIN : users.EMPLOYEE
  activePage.value = type === 'admin' ? 'admin' : 'votes'
  selectedVoteId.value = votes.value[0].id
}

function logout() {
  currentUser.value = null
  activePage.value = 'votes'
}

function hasSubmitted(voteId: string) {
  if (!currentUser.value) return false
  return submissions.value.some(s => s.voteId === voteId && s.userId === currentUser.value?.id)
}

function participationRate(vote: Vote) {
  return Math.round((vote.submissionCount / employeeCount.value) * 1000) / 10
}

function openVote(voteId: string) {
  selectedVoteId.value = voteId
  activePage.value = 'detail'
  submitError.value = ''
  draftAnswers.value = selectedVote.value.questions.map(q => ({ questionId: q.id, selectedOptionIds: [], textValue: '' }))
}

function openResult(voteId: string) {
  selectedVoteId.value = voteId
  activePage.value = 'results'
}

function answerOf(questionId: string) {
  let answer = draftAnswers.value.find(a => a.questionId === questionId)
  if (!answer) {
    answer = { questionId, selectedOptionIds: [], textValue: '' }
    draftAnswers.value.push(answer)
  }
  return answer
}

function scoreRange(question: Question) {
  const min = question.config.minScore || 1
  const max = question.config.maxScore || 5
  return Array.from({ length: max - min + 1 }, (_, i) => min + i)
}

function submitVote() {
  if (!currentUser.value || !selectedVote.value) return
  if (hasSubmitted(selectedVote.value.id)) {
    submitError.value = '你已提交该投票，无法重复提交。'
    confirmSubmit.value = false
    return
  }
  const invalid = selectedVote.value.questions.some(q => {
    const a = answerOf(q.id)
    if (!q.required) return false
    if (q.type === 'SINGLE_CHOICE') return !a.selectedOptionId
    if (q.type === 'MULTIPLE_CHOICE') return a.selectedOptionIds.length < (q.config.minSelected || 1)
    if (q.type === 'RATING') return !a.scoreValue
    return !a.textValue?.trim()
  })
  if (invalid) {
    submitError.value = '请完成所有必答题后再提交。'
    confirmSubmit.value = false
    return
  }
  submissions.value.push({ voteId: selectedVote.value.id, userId: currentUser.value.id, answers: JSON.parse(JSON.stringify(draftAnswers.value)), submittedAt: new Date().toLocaleString() })
  selectedVote.value.submissionCount += 1
  confirmSubmit.value = false
  activePage.value = 'votes'
}

function createVote() {
  const id = `v${Date.now()}`
  const questions: Question[] = newVote.structureType === 'SINGLE_QUESTION'
    ? [{ id: `${id}-q1`, type: 'SINGLE_CHOICE', title: '请选择你的意见', required: true, options: [{ id: `${id}-o1`, label: '同意' }, { id: `${id}-o2`, label: '不同意' }], config: {} }]
    : [
        { id: `${id}-q1`, type: 'SINGLE_CHOICE', title: '请选择你的首选方案', required: true, options: [{ id: `${id}-o1`, label: '方案 A' }, { id: `${id}-o2`, label: '方案 B' }], config: {} },
        { id: `${id}-q2`, type: 'MULTIPLE_CHOICE', title: '你关注哪些方面？', required: true, options: [{ id: `${id}-o3`, label: '效率' }, { id: `${id}-o4`, label: '成本' }, { id: `${id}-o5`, label: '体验' }], config: { minSelected: 1, maxSelected: 2 } },
        { id: `${id}-q3`, type: 'RATING', title: '请给本方案评分', required: true, options: [], config: { minScore: 1, maxScore: 5 } },
        { id: `${id}-q4`, type: 'TEXT', title: '请补充反馈', required: false, options: [], config: { maxLength: 500 } }
      ]
  votes.value.unshift({ id, title: newVote.title, description: newVote.description, mode: newVote.mode, structureType: newVote.structureType, status: 'ONGOING', endTime: newVote.endTime, questions, submissionCount: 0 })
}

function endVote(voteId: string) {
  const vote = votes.value.find(v => v.id === voteId)
  if (vote) vote.status = 'ENDED'
}

function choiceStats(question: Question) {
  return question.options.map((option, index) => {
    const base = [50, 30, 20, 12][index] || 8
    return { label: option.label, count: Math.round((selectedVote.value?.submissionCount || 0) * base / 100), percent: base }
  })
}

function ratingStats(question: Question) {
  return scoreRange(question).map(score => ({ score, count: [2, 8, 31, 82, 95][score - 1] || 0 }))
}

function ratingAverage(question: Question) {
  const stats = ratingStats(question)
  const total = stats.reduce((sum, item) => sum + item.count, 0)
  const weighted = stats.reduce((sum, item) => sum + item.score * item.count, 0)
  return total ? (weighted / total).toFixed(1) : '0.0'
}

function textFeedbacks(question: Question) {
  return ['希望会议室增加可视化预约状态。', '建议优化办公网络稳定性。', '希望跨部门协作流程更清晰。'].filter(Boolean)
}

function handleImport(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  importResult.errors = []
  if (!file) return
  if (!/\.(xlsx|xls|csv)$/i.test(file.name)) {
    importResult.total = 1
    importResult.success = 0
    importResult.failed = 1
    importResult.errors = ['仅支持 Excel 或 CSV 文件。']
    return
  }
  importResult.total = 500
  importResult.success = 486
  importResult.failed = 14
  importResult.errors = ['第 12 行：邮箱格式不正确', '第 27 行：工号重复', '第 38 行：角色字段缺失']
  employeeCount.value += importResult.success
}
</script>
