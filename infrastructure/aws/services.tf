resource "aws_db_subnet_group" "db" {

  name="${local.prefix}-db"
  subnet_ids=aws_subnet.private[*].id
  tags=local.tags
}
resource "aws_db_instance" "app" {

  identifier="${local.prefix}-db"
  engine="postgres"
  engine_version="17"
  instance_class="db.t4g.micro"
  allocated_storage=20
  max_allocated_storage=100
  db_name="reelnest"
  username="reelnest"
  manage_master_user_password=true
  db_subnet_group_name=aws_db_subnet_group.db.name
  vpc_security_group_ids=[aws_security_group.database.id]
  storage_encrypted=true
  publicly_accessible=false
  backup_retention_period=7
  deletion_protection=true
  skip_final_snapshot=false
  final_snapshot_identifier="${local.prefix}-db-final"
  auto_minor_version_upgrade=true
  tags=local.tags
}
resource "aws_lb" "main" {

  name="${local.prefix}-alb"
  internal=false
  load_balancer_type="application"
  subnets=aws_subnet.public[*].id
  security_groups=[aws_security_group.alb.id]
  drop_invalid_header_fields=true
  enable_deletion_protection=true
  tags=local.tags
}
resource "aws_lb_target_group" "api" {

  name="${local.prefix}-api"
  port=8080
  protocol="HTTP"
  vpc_id=aws_vpc.main.id
  target_type="ip"
  health_check {
    path="/actuator/health/liveness"
    matcher="200"
    interval=30
    healthy_threshold=2
  }
  tags=local.tags
}
resource "aws_lb_target_group" "web" {

  name="${local.prefix}-web"
  port=3000
  protocol="HTTP"
  vpc_id=aws_vpc.main.id
  target_type="ip"
  health_check {
    path="/"
    matcher="200-399"
    interval=30
    healthy_threshold=2
  }
  tags=local.tags
}
resource "aws_lb_listener" "https" {

  load_balancer_arn=aws_lb.main.arn
  port=443
  protocol="HTTPS"
  ssl_policy="ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn=var.alb_certificate_arn
  default_action {
    type="fixed-response"
    fixed_response {
      content_type="text/plain"
      message_body="Unknown host"
      status_code="404"
    }

  }
}
resource "aws_lb_listener_rule" "api" {

  listener_arn=aws_lb_listener.https.arn
  priority=10
  action {
    type="forward"
    target_group_arn=aws_lb_target_group.api.arn
  }
  condition {
    host_header {
      values=[local.api_host]
    }

  }
}
resource "aws_lb_listener_rule" "web" {

  listener_arn=aws_lb_listener.https.arn
  priority=20
  action {
    type="forward"
    target_group_arn=aws_lb_target_group.web.arn
  }
  condition {
    host_header {
      values=[local.app_host]
    }

  }
}
resource "aws_route53_record" "api" {

  zone_id=var.zone_id
  name=local.api_host
  type="A"
  alias {
    name=aws_lb.main.dns_name
    zone_id=aws_lb.main.zone_id
    evaluate_target_health=false
  }
}
resource "aws_route53_record" "web" {

  zone_id=var.zone_id
  name=local.app_host
  type="A"
  alias {
    name=aws_lb.main.dns_name
    zone_id=aws_lb.main.zone_id
    evaluate_target_health=false
  }
}
resource "aws_ecr_repository" "api" {
  name="${local.prefix}-api"
  image_scanning_configuration {
    scan_on_push=true
  }

  tags=local.tags
}
resource "aws_ecr_repository" "web" {
  name="${local.prefix}-web"
  image_scanning_configuration {
    scan_on_push=true
  }

  tags=local.tags
}
resource "aws_ecr_repository" "worker" {
  name="${local.prefix}-worker"
  image_scanning_configuration {
    scan_on_push=true
  }

  tags=local.tags
}
resource "aws_cloudwatch_log_group" "api" {
  name="/ecs/${local.prefix}/api"
  retention_in_days=30
  tags=local.tags
}
resource "aws_cloudwatch_log_group" "web" {
  name="/ecs/${local.prefix}/web"
  retention_in_days=30
  tags=local.tags
}
resource "aws_cloudwatch_log_group" "worker" {
  name="/ecs/${local.prefix}/worker"
  retention_in_days=30
  tags=local.tags
}
resource "aws_ecs_cluster" "main" {
  name="${local.prefix}-cluster"
  tags=local.tags
}
resource "aws_iam_role" "execution" {

  name="${local.prefix}-execution"
  assume_role_policy=jsonencode({
    Version="2012-10-17",Statement=[{
      Effect="Allow",Principal={
        Service="ecs-tasks.amazonaws.com"
      },Action="sts:AssumeRole"
    }]
  })
  tags=local.tags
}
resource "aws_iam_role_policy_attachment" "execution" {

  role=aws_iam_role.execution.name
  policy_arn="arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}
resource "aws_iam_role_policy" "execution_secrets" {

  name="read-restricted-secrets"
  role=aws_iam_role.execution.id
  policy=jsonencode({
    Version="2012-10-17",Statement=[{
      Effect="Allow",Action=["secretsmanager:GetSecretValue"],Resource=[aws_secretsmanager_secret.cdn_key.arn,aws_secretsmanager_secret.runtime_db.arn]
    }]
  })
}
resource "aws_iam_role" "app" {

  name="${local.prefix}-app"
  assume_role_policy=aws_iam_role.execution.assume_role_policy
  tags=local.tags
}
resource "aws_iam_role_policy" "media_access" {

  name="media-bucket-access"
  role=aws_iam_role.app.id
  policy=jsonencode({
    Version="2012-10-17",Statement=[
    {
      Effect="Allow",Action=["s3:ListBucket","s3:ListBucketMultipartUploads"],Resource=[aws_s3_bucket.source.arn,aws_s3_bucket.processed.arn]
    },
    {
      Effect="Allow",Action=["s3:GetObject","s3:PutObject","s3:AbortMultipartUpload","s3:ListMultipartUploadParts"],Resource=["${aws_s3_bucket.source.arn}/*","${aws_s3_bucket.processed.arn}/*"]
    }
    ]
  })
}
# Created without a value; operator must provision the runtime DB role in the
# database and populate this secret before ECS services are enabled.
resource "aws_secretsmanager_secret" "runtime_db" {

  name="${local.prefix}-runtime-db-password"
  description="Runtime SQL login only; provision via scripts/sql/provision_runtime_role.sql"
  tags=local.tags
}
resource "aws_secretsmanager_secret" "cdn_key" {

  name="${local.prefix}-cloudfront-private-key"
  description="Populate separately with PKCS8 PEM; never commit key or put its value into Terraform state"
  tags=local.tags
}
locals {

  common_env = [
  {
    name="DB_URL",value="jdbc:postgresql://${aws_db_instance.app.address}:5432/reelnest"
  },
  {
    name="DB_USER",value="reelnest_app"
  },
  {
    name="SPRING_FLYWAY_ENABLED",value="false"
  },
  {
    name="REDIS_HOST",value=aws_elasticache_replication_group.redis.primary_endpoint_address
  },
  {
    name="REDIS_SSL_ENABLED",value="true"
  },
  {
    name="JWT_ISSUER_URI",value=var.identity_issuer_url
  },
  {
    name="JWT_JWKS_URI",value=var.identity_jwks_uri
  },
  {
    name="CORS_ORIGIN",value="https://${local.app_host}"
  },
  {
    name="S3_REGION",value=var.aws_region
  },
  {
    name="S3_SOURCE_BUCKET",value=aws_s3_bucket.source.bucket
  },
  {
    name="S3_PROCESSED_BUCKET",value=aws_s3_bucket.processed.bucket
  },
  {
    name="PLAYBACK_DEV_PROXY_ENABLED",value="false"
  },
  {
    name="PLAYBACK_CDN_BASE_URL",value="https://${local.stream_host}"
  },
  {
    name="PLAYBACK_CDN_COOKIE_DOMAIN",value=".${var.root_domain}"
  },
  {
    name="CLOUDFRONT_KEY_PAIR_ID",value=aws_cloudfront_public_key.media.id
  },
  # CloudFront country allowlist is configured, but operator must verify geo behavior before attesting.
  {
    name="PLAYBACK_TRUSTED_COUNTRY",value=var.playback_country
  },
  {
    name="PLAYBACK_CDN_GEO_REVIEWED",value=tostring(var.reviewed_geo_enforcement)
  },
  {
    name="APP_DEPLOY_MODE",value=var.environment
  },
  {
    name="BILLING_RECONCILIATION_ENABLED",value="false"
  }
  ]
  common_db_secret={
    name="DB_PASSWORD",valueFrom=aws_secretsmanager_secret.runtime_db.arn
  }
  log_opts={
    awslogs-region=var.aws_region,awslogs-stream-prefix="ecs"
  }
}
resource "aws_ecs_task_definition" "api" {

  family="${local.prefix}-api"
  network_mode="awsvpc"
  requires_compatibilities=["FARGATE"]
  cpu="512"
  memory="1024"
  execution_role_arn=aws_iam_role.execution.arn
  task_role_arn=aws_iam_role.app.arn
  container_definitions=jsonencode([{

    name="api",image=var.api_image,essential=true,
    portMappings=[{
      containerPort=8080,hostPort=8080,protocol="tcp"
    }],
    environment=local.common_env,
    secrets=[local.common_db_secret,{
      name="CLOUDFRONT_PRIVATE_KEY_PEM",valueFrom=aws_secretsmanager_secret.cdn_key.arn
    }],
    logConfiguration={
      logDriver="awslogs",options=merge(local.log_opts,{
        awslogs-group=aws_cloudwatch_log_group.api.name
      })
    }

  }])
  tags=local.tags
}
resource "aws_ecs_task_definition" "web" {

  family="${local.prefix}-web"
  network_mode="awsvpc"
  requires_compatibilities=["FARGATE"]
  cpu="512"
  memory="1024"
  execution_role_arn=aws_iam_role.execution.arn
  container_definitions=jsonencode([{

    name="web",image=var.web_image,essential=true,
    portMappings=[{
      containerPort=3000,hostPort=3000,protocol="tcp"
    }],
    environment=[{
      name="NODE_ENV",value="production"
    },{
      name="PORT",value="3000"
    }],
    logConfiguration={
      logDriver="awslogs",options=merge(local.log_opts,{
        awslogs-group=aws_cloudwatch_log_group.web.name
      })
    }

  }])
  tags=local.tags
}
resource "aws_ecs_task_definition" "worker" {

  family="${local.prefix}-worker"
  network_mode="awsvpc"
  requires_compatibilities=["FARGATE"]
  cpu="1024"
  memory="2048"
  execution_role_arn=aws_iam_role.execution.arn
  task_role_arn=aws_iam_role.app.arn
  container_definitions=jsonencode([{

    name="worker",image=var.worker_image,essential=true,
    environment=[{
      name="DB_HOST",value=aws_db_instance.app.address
    },{
      name="DB_USER",value="reelnest_app"
    },
    {
      name="DB_NAME",value="reelnest"
    },{
      name="S3_REGION",value=var.aws_region
    },
    {
      name="SOURCE_BUCKET",value=aws_s3_bucket.source.bucket
    },{
      name="PROCESSED_BUCKET",value=aws_s3_bucket.processed.bucket
    }],
    secrets=[local.common_db_secret],
    logConfiguration={
      logDriver="awslogs",options=merge(local.log_opts,{
        awslogs-group=aws_cloudwatch_log_group.worker.name
      })
    }

  }])
  tags=local.tags
}
resource "aws_ecs_service" "api" {

  count=var.enable_services?1:0
  name="api"
  cluster=aws_ecs_cluster.main.id
  task_definition=aws_ecs_task_definition.api.arn
  desired_count=1
  launch_type="FARGATE"
  health_check_grace_period_seconds=120
  network_configuration {
    subnets=aws_subnet.private[*].id
    security_groups=[aws_security_group.ecs.id]
    assign_public_ip=false
  }
  load_balancer {
    target_group_arn=aws_lb_target_group.api.arn
    container_name="api"
    container_port=8080
  }
  depends_on=[aws_lb_listener_rule.api]
  lifecycle {

    precondition {

      condition = var.reviewed_geo_enforcement
      error_message = "Review actual CloudFront geofencing and bypass tests before ECS deployment"

    }

  }
  tags=local.tags
}
resource "aws_ecs_service" "web" {

  count=var.enable_services?1:0
  name="web"
  cluster=aws_ecs_cluster.main.id
  task_definition=aws_ecs_task_definition.web.arn
  desired_count=1
  launch_type="FARGATE"
  health_check_grace_period_seconds=120
  network_configuration {
    subnets=aws_subnet.private[*].id
    security_groups=[aws_security_group.ecs.id]
    assign_public_ip=false
  }
  load_balancer {
    target_group_arn=aws_lb_target_group.web.arn
    container_name="web"
    container_port=3000
  }
  depends_on=[aws_lb_listener_rule.web]
  tags=local.tags
}
resource "aws_ecs_service" "worker" {

  count=var.enable_services?1:0
  name="worker"
  cluster=aws_ecs_cluster.main.id
  task_definition=aws_ecs_task_definition.worker.arn
  desired_count=1
  launch_type="FARGATE"
  network_configuration {
    subnets=aws_subnet.private[*].id
    security_groups=[aws_security_group.ecs.id]
    assign_public_ip=false
  }
  tags=local.tags
}
# Encrypted, private Redis cache/rate-limit state. For production consider Redis auth/IAM
# and Multi-AZ failover; this starter intentionally targets a staging deployment.
resource "aws_security_group" "redis" {

  name="${local.prefix}-redis"
  vpc_id=aws_vpc.main.id
  ingress {
    from_port=6379
    to_port=6379
    protocol="tcp"
    security_groups=[aws_security_group.ecs.id]
  }
  egress {
    from_port=0
    to_port=0
    protocol="-1"
    cidr_blocks=["0.0.0.0/0"]
  }
  tags=local.tags
}
resource "aws_elasticache_subnet_group" "redis" {

  name="${local.prefix}-redis"
  subnet_ids=aws_subnet.private[*].id
  tags=local.tags
}
resource "aws_elasticache_replication_group" "redis" {

  replication_group_id="${local.prefix}-redis"
  description="ReelNest staging Redis cache and API admission limiter"
  engine="redis"
  engine_version="7.1"
  node_type="cache.t4g.micro"
  num_cache_clusters=1
  automatic_failover_enabled=false
  at_rest_encryption_enabled=true
  transit_encryption_enabled=true
  snapshot_retention_limit=1
  subnet_group_name=aws_elasticache_subnet_group.redis.name
  security_group_ids=[aws_security_group.redis.id]
  tags=local.tags
}
resource "aws_wafv2_web_acl" "alb" {

  name="${local.prefix}-api-rate-limit"
  scope="REGIONAL"
  default_action {
    allow {
    }

  }
  rule {

    name="excessive-requests-per-ip"
    priority=1
    action {
      block {
      }

    }
    statement {
      rate_based_statement {
        limit=1200
        aggregate_key_type="IP"
      }

    }
    visibility_config {
      cloudwatch_metrics_enabled=true
      metric_name="reelnest-rate-limit"
      sampled_requests_enabled=true
    }

  }
  visibility_config {
    cloudwatch_metrics_enabled=true
    metric_name="reelnest-web-acl"
    sampled_requests_enabled=false
  }
  tags=local.tags
}
resource "aws_wafv2_web_acl_association" "alb" {

  resource_arn=aws_lb.main.arn
  web_acl_arn=aws_wafv2_web_acl.alb.arn
}
resource "aws_cloudwatch_metric_alarm" "api_5xx" {

  alarm_name="${local.prefix}-api-5xx"
  comparison_operator="GreaterThanOrEqualToThreshold"
  evaluation_periods=2
  metric_name="HTTPCode_Target_5XX_Count"
  namespace="AWS/ApplicationELB"
  period=300
  statistic="Sum"
  threshold=10
  alarm_description="API target group is returning elevated 5xx errors"
  dimensions={
    LoadBalancer=aws_lb.main.arn_suffix,TargetGroup=aws_lb_target_group.api.arn_suffix
  }
  tags=local.tags
}
