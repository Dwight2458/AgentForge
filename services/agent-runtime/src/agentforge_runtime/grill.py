from .models import GrillRequest, GrillResponse


def generate_grill_questions(request: GrillRequest, limit: int = 3) -> GrillResponse:
    text = request.request.lower()
    candidates: list[str] = []

    if any(term in text for term in ("页面", "frontend", "ui", "page", "vue")):
        candidates.append("Which user-visible states and URL persistence behavior are required?")
    if any(term in text for term in ("接口", "api", "backend", "spring")):
        candidates.append("Which API compatibility constraints and default behavior must be preserved?")
    if any(term in text for term in ("database", "数据库", "migration", "schema")):
        candidates.append("Does this task require a backward-compatible database migration or data backfill?")

    candidates.extend(
        [
            "Which edge cases would make an otherwise plausible implementation unacceptable?",
            "Which automated checks must pass before AgentForge may deliver the change?",
            "Are any files, modules, dependencies, or public contracts explicitly out of scope?",
        ]
    )

    questions = list(dict.fromkeys(candidates))[:limit]
    return GrillResponse(questions=questions)

