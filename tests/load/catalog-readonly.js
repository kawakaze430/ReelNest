// k6 read-only API smoke/load scenario. Never use public domain without authorization.
import http from 'k6/http';
import {check, sleep} from 'k6';
export const options = {
  scenarios: {catalog_readonly:{executor:'ramping-vus',startVUs:0,
    stages:[{duration:'20s',target:5},{duration:'45s',target:20},{duration:'20s',target:0}],
    gracefulRampDown:'10s'}},
  thresholds: {'http_req_failed':['rate<0.01'],'http_req_duration':['p(95)<800']}
};
const BASE=__ENV.REELNEST_API || 'http://localhost:8080';
export default function () {
  const res=http.get(`${BASE}/api/v1/catalog?size=12`,{tags:{name:'catalog'}});
  check(res, {'catalog HTTP 200':r=>r.status===200,'catalog page':r=>r.body?.includes('content')});
  sleep(1);
}
