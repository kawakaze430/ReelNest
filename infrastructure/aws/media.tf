resource "aws_s3_bucket" "source" {
  bucket = "${local.prefix}-source-${data.aws_caller_identity.current.account_id}"
  tags = local.tags
}
resource "aws_s3_bucket" "processed" {
  bucket = "${local.prefix}-processed-${data.aws_caller_identity.current.account_id}"
  tags = local.tags
}
data "aws_caller_identity" "current" {
}
resource "aws_s3_bucket_public_access_block" "source" {

  bucket=aws_s3_bucket.source.id
  block_public_acls=true
  block_public_policy=true
  ignore_public_acls=true
  restrict_public_buckets=true
}
resource "aws_s3_bucket_public_access_block" "processed" {

  bucket=aws_s3_bucket.processed.id
  block_public_acls=true
  block_public_policy=true
  ignore_public_acls=true
  restrict_public_buckets=true
}
resource "aws_s3_bucket_server_side_encryption_configuration" "source" {

  bucket=aws_s3_bucket.source.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm="AES256"
    }

  }
}
resource "aws_s3_bucket_server_side_encryption_configuration" "processed" {

  bucket=aws_s3_bucket.processed.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm="AES256"
    }

  }
}
resource "aws_s3_bucket_cors_configuration" "source" {

  bucket=aws_s3_bucket.source.id
  cors_rule {

    allowed_headers=["*"]
    allowed_methods=["PUT"]
    allowed_origins=["https://${local.app_host}"]
    expose_headers=["ETag"]
    max_age_seconds=600

  }
}
resource "aws_s3_bucket_lifecycle_configuration" "source" {

  bucket=aws_s3_bucket.source.id
  rule {
    id="abort-incomplete"
    status="Enabled"
    abort_incomplete_multipart_upload {
      days_after_initiation=2
    }

  }
}
resource "aws_cloudfront_origin_access_control" "media" {

  name="${local.prefix}-media-oac"
  description="Private media bucket"
  origin_access_control_origin_type="s3"
  signing_behavior="always"
  signing_protocol="sigv4"
}
resource "aws_cloudfront_public_key" "media" {

  name="${local.prefix}-media-key"
  encoded_key=var.cdn_public_key_pem
  comment="Public half of externally managed CloudFront signing key"
}
resource "aws_cloudfront_key_group" "media" {

  name="${local.prefix}-media-keys"
  items=[aws_cloudfront_public_key.media.id]
}
resource "aws_cloudfront_response_headers_policy" "media" {

  name="${local.prefix}-media-cors"
  cors_config {

    access_control_allow_credentials=true
    access_control_allow_headers {
      items=["*"]
    }
    access_control_allow_methods {
      items=["GET","HEAD","OPTIONS"]
    }
    access_control_allow_origins {
      items=["https://${local.app_host}"]
    }
    origin_override=true

  }
  security_headers_config {

    content_type_options {
      override=true
    }
    frame_options {
      frame_option="DENY"
      override=true
    }
    referrer_policy {
      referrer_policy="no-referrer"
      override=true
    }
    strict_transport_security {
      access_control_max_age_sec=31536000
      include_subdomains=true
      override=true
    }

  }
}
resource "aws_cloudfront_distribution" "media" {

  enabled=true
  aliases=[local.stream_host]
  origin {

    domain_name=aws_s3_bucket.processed.bucket_regional_domain_name
    origin_id="media-s3"
    origin_access_control_id=aws_cloudfront_origin_access_control.media.id

  }
  default_cache_behavior {

    target_origin_id="media-s3"
    viewer_protocol_policy="https-only"
    allowed_methods=["GET","HEAD","OPTIONS"]
    cached_methods=["GET","HEAD"]
    trusted_key_groups=[aws_cloudfront_key_group.media.id]
    cache_policy_id="658327ea-f89d-4fab-a63d-7e88639e58f6" # AWS managed CachingOptimized
    response_headers_policy_id=aws_cloudfront_response_headers_policy.media.id
    compress=true

  }
  # A country-specific geofence at the CDN edge. No unreviewed global streaming.
  # For multi-country expansion implement per-request verified country assertions.
  restrictions {

    geo_restriction {

      restriction_type = "whitelist"
      locations = [var.playback_country]

    }

  }
  viewer_certificate {
    acm_certificate_arn=var.cdn_certificate_arn
    ssl_support_method="sni-only"
    minimum_protocol_version="TLSv1.2_2021"
  }
  tags=local.tags
  depends_on=[aws_s3_bucket_public_access_block.processed]
}
data "aws_iam_policy_document" "media_bucket" {

  statement {

    sid="AllowCloudFrontOnly"
    actions=["s3:GetObject"]
    resources=["${aws_s3_bucket.processed.arn}/*"]
    principals {
      type="Service"
      identifiers=["cloudfront.amazonaws.com"]
    }
    condition {
      test="StringEquals"
      variable="AWS:SourceArn"
      values=[aws_cloudfront_distribution.media.arn]
    }

  }
}
resource "aws_s3_bucket_policy" "processed" {

  bucket=aws_s3_bucket.processed.id
  policy=data.aws_iam_policy_document.media_bucket.json
}
resource "aws_route53_record" "stream" {

  zone_id=var.zone_id
  name=local.stream_host
  type="A"
  alias {
    name=aws_cloudfront_distribution.media.domain_name
    zone_id=aws_cloudfront_distribution.media.hosted_zone_id
    evaluate_target_health=false
  }
}
