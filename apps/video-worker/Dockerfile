FROM python:3.12-slim
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g 10001 worker && useradd -m -u 10001 -g worker worker
WORKDIR /work
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
# Include all first-party modules required transitively by worker/pipeline.
# The old two-file image could build but fail on its very first import.
COPY *.py ./
# Fail the image build if the dependency graph is not importable.
RUN python -c "import worker, pipeline, caption_packaging, timed_text, audio_packaging, preview_artwork"
USER worker
ENV PYTHONUNBUFFERED=1
CMD ["python", "worker.py"]
