output "app_url" {
  value="https://${local.app_host}"
}
output "api_url" {
  value="https://${local.api_host}"
}
output "media_url" {
  value="https://${local.stream_host}"
}
output "source_bucket" {
  value=aws_s3_bucket.source.bucket
}
output "processed_bucket" {
  value=aws_s3_bucket.processed.bucket
}
output "ecr_repositories" {
  value={
    api=aws_ecr_repository.api.repository_url,web=aws_ecr_repository.web.repository_url,worker=aws_ecr_repository.worker.repository_url
  }

}
output "cdn_private_key_secret_arn" {
  value=aws_secretsmanager_secret.cdn_key.arn
}
output "cloudfront_public_key_id" {
  value=aws_cloudfront_public_key.media.id
}
output "db_host" {
  value=aws_db_instance.app.address
}

output "runtime_db_secret_arn" {
  value=aws_secretsmanager_secret.runtime_db.arn
}
