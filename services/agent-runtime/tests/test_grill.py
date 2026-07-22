from agentforge_runtime.grill import generate_grill_questions
from agentforge_runtime.models import GrillRequest


def test_grill_targets_relevant_ambiguities_and_respects_limit() -> None:
    response = generate_grill_questions(
        GrillRequest(request="Add a Vue page backed by a Spring API and database migration"),
        limit=3,
    )

    assert len(response.questions) == 3
    assert any("URL" in question for question in response.questions)
    assert any("API" in question for question in response.questions)
    assert any("database migration" in question for question in response.questions)

