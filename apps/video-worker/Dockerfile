FROM python:3.12-slim
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g 10001 worker && useradd -m -u 10001 -g worker worker
WORKDIR /work
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY pipeline.py worker.py ./
USER worker
ENV PYTHONUNBUFFERED=1
CMD ["python", "worker.py"]
