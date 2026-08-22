import unittest
from unittest.mock import AsyncMock, patch

from app.schemas.memory_ai import InterviewNextRequest, InterviewSummarizeRequest, InterviewTurn
from app.services.memory_ai_service import create_interview_summary, generate_next_question


class InterviewServiceTest(unittest.IsolatedAsyncioTestCase):
    async def test_first_question_does_not_require_llm(self):
        result = await generate_next_question(InterviewNextRequest(narrator_name="王阿姨", theme="童年"))
        self.assertIn("童年", result.question)
        self.assertFalse(result.degraded)

    async def test_next_question_degrades_on_invalid_json(self):
        request = InterviewNextRequest(
            narrator_name="王阿姨",
            theme="童年",
            turns=[InterviewTurn(role="assistant", content="想从哪里讲起？"), InterviewTurn(role="user", content="从老家讲起。")],
        )
        with patch("app.services.memory_ai_service.call_llm", new=AsyncMock(return_value="not-json")):
            result = await generate_next_question(request)
        self.assertTrue(result.degraded)
        self.assertTrue(result.question)

    async def test_summary_keeps_only_valid_user_sources_and_verbatim_quote(self):
        request = InterviewSummarizeRequest(
            narrator_name="王阿姨",
            relation="妈妈",
            theme="家常菜",
            turns=[
                InterviewTurn(role="assistant", content="最难忘哪道菜？"),
                InterviewTurn(role="user", content="我最难忘外婆教我的青团，清明前会一起做。"),
            ],
        )
        response = '{"profile":{"bio":"会做青团。","traits":["家常菜","抑郁症"]},"memories":[{"title":"清明青团","summary":"外婆教做青团。","content":"外婆教她做青团。","topics":["清明"],"quote":"模型编造的话","source_turns":[1]},{"title":"无来源","content":"不能保留","source_turns":[0]}]}'
        with patch("app.services.memory_ai_service.call_llm", new=AsyncMock(return_value=response)):
            result = await create_interview_summary(request)
        self.assertEqual(result.profile.traits, ["家常菜"])
        self.assertEqual(len(result.memories), 1)
        self.assertIsNone(result.memories[0].quote)
        self.assertEqual(result.memories[0].source_turns, [1])

    async def test_summary_without_answers_returns_empty_degraded_result(self):
        request = InterviewSummarizeRequest(narrator_name="王阿姨", turns=[])
        result = await create_interview_summary(request)
        self.assertTrue(result.degraded)
        self.assertEqual(result.memories, [])

    async def test_skipped_turn_is_not_profile_evidence(self):
        request = InterviewSummarizeRequest(
            narrator_name="王阿姨",
            turns=[InterviewTurn(role="user", content="我想跳过这个问题。")],
        )
        result = await create_interview_summary(request)
        self.assertTrue(result.degraded)
        self.assertEqual(result.memories, [])


if __name__ == "__main__":
    unittest.main()
