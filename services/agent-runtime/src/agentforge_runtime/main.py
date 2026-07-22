import uvicorn


def run() -> None:
    uvicorn.run("agentforge_runtime.app:app", host="0.0.0.0", port=8081)


if __name__ == "__main__":
    run()
