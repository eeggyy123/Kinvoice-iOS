import unittest

from fastapi.testclient import TestClient

from app.main import app


class FocusedAPITest(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_health(self):
        response = self.client.get("/health")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["status"], "ok")

    def test_only_focused_routes_are_published(self):
        paths = set(app.openapi()["paths"])
        self.assertIn("/v1/memories/draft", paths)
        self.assertIn("/v1/knowledge/ask", paths)
        self.assertNotIn("/tts", paths)
        self.assertNotIn("/replica/create", paths)
        self.assertNotIn("/chat", paths)


if __name__ == "__main__":
    unittest.main()
