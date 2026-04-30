FROM python:3.12-slim

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && groupadd --system ruleforge \
    && useradd --system --gid ruleforge --home-dir /app --shell /usr/sbin/nologin ruleforge \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY --chown=ruleforge:ruleforge . .

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8000/health || exit 1

USER ruleforge

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
