# ReelNest — worldwide latency & resilience strategy (design; not deployed)

1. Start with **one application AWS region + global CloudFront CDN** serving territories where rights exist. No multi-region DB writes before measured need.
2. Collect opt-in, aggregated startup delay, rebuffering, video errors, cache hit ratio and edge/origin egress by broad geography, device and ISP. Set privacy thresholds and retention before collection.
3. Separate content rights from app availability: permit landing/catalog access where lawful, restrict individual title authorization independently.
4. Set explicit RPO/RTO objectives with the owner before selecting warm standby, PostgreSQL replicas or multi-region disaster recovery. Rehearse restores and failover on staging.
5. Protect originals using private S3/OAC, lifecycle management and regional redundancy only when required by cost or business continuity analysis.
6. Treat production geolocation from CDN and server as an explicit trust boundary; do not accept viewer-provided country. Test IPv4/IPv6, proxies and territory edge conditions.
7. Configure health/readiness endpoints, autoscaling based on real bottlenecks, WAF and error budgets. Keep FFmpeg worker queues independently scalable.
8. Optimize video bitrate ladder and CloudFront cache hit before deploying global compute regions; measure launch cohorts rather than assume worldwide latency requirements.
9. Roll out countries individually after verifying licence contracts, taxation, payments, translations, customer support and content ratings for each territory.
10. Multi-region active/active APIs, regional rights caches and cross-region DB failover remain P2 design choices until actual load and recovery needs are measured.
