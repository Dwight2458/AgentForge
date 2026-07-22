# AgentForge UI design system

## Concept references

- `docs/design/projects.png`: project selection, task creation, recent runs.
- `docs/design/grill.png`: requirement clarification and live requirement spec.
- `docs/design/run-console.png`: execution timeline, subagent topology, diff, tests, terminal, and budget.

The three references are a coordinated desktop system. The shared left navigation is `Projects`, `Tasks`, `Runs`, `Skills`, `Settings`; the active item changes by route. The angular orange `A` and `AgentForge` wordmark are code-native.

## Visual direction

- Background: true white `#ffffff`; never replace it with cream or warm gray.
- Navigation: graphite `#11161d`; selected surface `#242b34`.
- Primary text: `#17191d`; secondary text: `#646b75`; borders: `#dfe3e8`.
- Brand accent: forge orange `#f4511e`; hover `#dc3f0f`; focus ring `rgba(244, 81, 30, .25)`.
- Success: `#159447`; running: brand orange; warning: `#c77900`; error: `#d92d20`.
- Terminal: `#0d1014` with `#e8ebef` text and muted `#99a2ad` metadata.
- UI font: Inter with system sans-serif fallback. Code and terminal: JetBrains Mono with monospace fallback.
- Spacing follows an 8 px scale. Controls are 36–40 px high. Panels use 6 px radii, 1 px borders, and no decorative shadows.
- Motion is limited to progress transitions, streaming event insertion, and active-state changes; it must honor `prefers-reduced-motion`.

## Component and container rules

- `AppShell` owns the 208 px desktop navigation rail and the responsive drawer below 900 px.
- `StageRail` is shared by Grill and Run and always uses `Clarify`, `Plan`, `Run`, `Verify`, `Deliver` in that order.
- Dense content uses tables, rows, rails, split panes, and tabs. Do not convert it into a card grid.
- Buttons have primary, secondary, danger, and disabled variants. Icons use one consistent 1.75 px outline family.
- Status is communicated with icon, color, and text together; color alone is insufficient.
- The run console uses a 62/38 split above 1180 px, stacks below that width, and keeps the terminal at least 220 px tall.
- The Grill screen uses a conversation/spec split; on narrow screens the requirement spec becomes a drawer reachable from a persistent button.

## Allowed visible copy

### Navigation

`AgentForge`, `Projects`, `Tasks`, `Runs`, `Skills`, `Settings`, `Help`.

### Projects

`Projects`, `Repositories`, `Filter repositories…`, `New task`, `Repository`, `Development request`, `Describe the feature, bug fix, or improvement you want to build…`, `Resource profile`, `Estimated runtime`, `Start clarification`, `Recent runs`, `View all runs`.

### Grill

`Clarify the task`, `Requirement Agent is asking questions to fully understand the request.`, `Requirement Spec`, `Goal`, `Acceptance criteria`, `Constraints`, `Test plan`, `Answer the question or add more details…`, `Save draft`, `Finalize and run automatically`.

### Run console

`Execution timeline`, `Live`, `Filter`, `Subagent topology`, `View logs`, `Diff`, `Tests`, `Artifacts`, `Changed files`, `Live terminal`, `Clear`, `Cancel run`, `Open preview`, `Elapsed`, `Tool calls`, `Tokens`, `Budget`.

Do not add hero copy, AI claims, decorative badges, fake metrics, or approval language.

## Fidelity inventory

- Projects: left repository list, right task composer, and full-width recent-runs table must all remain visible at 1440×900.
- Grill: stage rail, conversation, structured spec inspector, composer, and final automatic-run action must fit without hidden primary controls.
- Run: stage rail, timeline, terminal, topology, diff/tests/artifacts, and budget footer must remain simultaneously legible at 1440×900.
- Responsive continuation must preserve workflow order rather than merely scale the desktop surface.
- Central UI text, tables, controls, topology, terminal, and brand are HTML/CSS/SVG; the PNG files are specification references only and must never be shipped as the application UI.

## Intentional implementation constraints

- Initial data is local typed fixture data so the UI remains demonstrable before backend endpoints are complete.
- The logo mark is a small code-native SVG matching the angular forged `A`; no generated bitmap is used in production UI.
- Generated concepts occasionally show compact status tags. Implementation may use them only when they communicate real state, never as decoration.
