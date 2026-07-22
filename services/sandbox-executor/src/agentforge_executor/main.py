import uvicorn


def run() -> None:
    uvicorn.run("agentforge_executor.app:app", host="0.0.0.0", port=8082)


if __name__ == "__main__":
    run()
