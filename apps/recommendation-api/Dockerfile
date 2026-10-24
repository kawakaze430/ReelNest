FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN --mount=type=secret,id=proxy_ca \
    if [ -f /run/secrets/proxy_ca ]; then export PIP_CERT=/run/secrets/proxy_ca; fi; \
    pip install --no-cache-dir -r requirements.txt && useradd --create-home --uid 10001 appuser
COPY --chmod=0644 *.py ./
# Host-side tests can import files that an incomplete image omitted. Verify the
# deployed entry point and its transitive first-party imports before shipping it.
RUN python -c "import main"
USER 10001
EXPOSE 8000
CMD ["uvicorn","main:app","--host","0.0.0.0","--port","8000","--workers","1"]
