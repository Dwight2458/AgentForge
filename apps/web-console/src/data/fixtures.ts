import type { GrillMessage, RecentRun, Repository, SpecSection, TimelineEvent } from '@/types'

export const repositories: Repository[] = [
  {
    id: 'mall-service',
    fullName: 'dwight2458/mall-service',
    description: 'Spring Boot + Vue commerce demo',
    branch: 'main',
  },
  {
    id: 'agentforge',
    fullName: 'dwight2458/AgentForge',
    description: 'Autonomous software engineering platform',
    branch: 'main',
  },
  {
    id: 'order-worker',
    fullName: 'dwight2458/order-worker',
    description: 'Order processing and payment events',
    branch: 'develop',
  },
  {
    id: 'platform-charts',
    fullName: 'dwight2458/platform-charts',
    description: 'Helm charts for local services',
    branch: 'main',
  },
]

export const recentRuns: RecentRun[] = [
  {
    id: 'AF-1041',
    task: 'Repair flaky order repository test',
    repository: 'dwight2458/mall-service',
    status: 'Succeeded',
    started: '18 minutes ago',
    duration: '8m 42s',
  },
  {
    id: 'AF-1040',
    task: 'Update Spring Boot dependencies',
    repository: 'dwight2458/order-worker',
    status: 'Succeeded',
    started: 'Today, 09:47',
    duration: '12m 13s',
  },
  {
    id: 'AF-1039',
    task: 'Add checkout browser coverage',
    repository: 'dwight2458/mall-service',
    status: 'Failed',
    started: 'Yesterday, 17:20',
    duration: '31m 05s',
  },
]

export const initialMessages: GrillMessage[] = [
  {
    id: 1,
    role: 'agent',
    author: 'Requirement Agent',
    time: '10:21',
    content: 'Which payment statuses should be included in the filter?',
  },
  {
    id: 2,
    role: 'user',
    author: 'You',
    time: '10:21',
    content: 'Include PENDING, AUTHORIZED, CAPTURED, FAILED, REFUNDED, and VOIDED.',
  },
  {
    id: 3,
    role: 'agent',
    author: 'Requirement Agent',
    time: '10:22',
    content: 'Where should the filter be placed in the UI?',
  },
  {
    id: 4,
    role: 'user',
    author: 'You',
    time: '10:22',
    content: 'Above the transactions table, aligned to the left of the search input.',
  },
  {
    id: 5,
    role: 'agent',
    author: 'Requirement Agent',
    time: '10:23',
    content: 'What should the default selection and URL behavior be?',
  },
  {
    id: 6,
    role: 'user',
    author: 'You',
    time: '10:23',
    content: 'Default to All statuses. Persist changes in the URL query parameters.',
  },
]

export const specSections: SpecSection[] = [
  {
    title: 'Goal',
    items: ['Allow users to filter orders by payment status.'],
  },
  {
    title: 'Acceptance criteria',
    items: [
      'The API accepts an optional paymentStatus parameter.',
      'A status filter is visible above the orders table.',
      'The selected status persists in the URL query parameters.',
      'Existing pagination and response fields remain unchanged.',
    ],
  },
  {
    title: 'Constraints',
    items: [
      'Keep the current API response shape backward compatible.',
      'Use existing design-system controls.',
      'Do not modify unrelated checkout behavior.',
    ],
  },
  {
    title: 'Test plan',
    items: [
      'Run backend repository and controller tests.',
      'Run frontend type checking and unit tests.',
      'Verify filter state with Playwright and a page reload.',
    ],
  },
]

export const timelineEvents: TimelineEvent[] = [
  {
    id: 1,
    time: '12:21:03',
    agent: 'Repository Analyst',
    summary: 'Scanned repository and detected Spring Boot, Vue 3, and PostgreSQL',
    detail: '1,842 files · 6 modules',
    status: 'complete',
  },
  {
    id: 2,
    time: '12:21:09',
    agent: 'Repository Analyst',
    summary: 'Built a focused context pack for order query and list modules',
    detail: '12 symbols · 7 tests',
    status: 'complete',
  },
  {
    id: 3,
    time: '12:21:18',
    agent: 'Supervisor',
    summary: 'Created a dependency-aware execution plan',
    detail: '4 tasks',
    status: 'complete',
  },
  {
    id: 4,
    time: '12:21:25',
    agent: 'Backend Developer',
    summary: 'Updated the order query to support paymentStatus',
    detail: 'OrderQueryService.java · +24 −6',
    status: 'complete',
  },
  {
    id: 5,
    time: '12:22:03',
    agent: 'Backend Developer',
    summary: 'Added controller and repository coverage',
    detail: '18 tests passed',
    status: 'complete',
  },
  {
    id: 6,
    time: '12:22:37',
    agent: 'Frontend Developer',
    summary: 'Adding the status filter and URL synchronization',
    detail: 'OrderListView.vue',
    status: 'running',
  },
  {
    id: 7,
    time: '—',
    agent: 'Test Engineer',
    summary: 'Integration and browser verification',
    status: 'pending',
  },
]

