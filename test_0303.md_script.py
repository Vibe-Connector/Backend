import json
import requests
import io
import sys
import yaml

with open('/tmp/api-docs.json') as f:
    api_docs = json.load(f)

# Hardcoded Developer A API endpoints to test (approximately matching the provided list)
# Note: we might not find exact endpoints if the actual implementation uses different paths.
# As seen from the previous curl result, some implemented paths are:
# /api/v1/options, /api/v1/options/moods, /api/v1/auth/signup, /api/v1/auth/login,
# /api/v1/vibes/sessions, /api/v1/users/me, etc.

target_endpoints = {
    "/api/v1/auth/signup": ['post'],
    "/api/v1/auth/login": ['post'],
    "/api/v1/auth/social/{provider}": ['post'],
    "/api/v1/auth/refresh": ['post'],
    "/api/v1/auth/logout": ['post'],
    "/api/v1/auth/check-email": ['get'],
    "/api/v1/auth/check-nickname": ['get'],
    "/api/v1/users/me": ['get', 'put', 'delete'],
    "/api/v1/users/me/profile-image": ['post'],
    "/api/v1/users/me/settings": ['get', 'put'],
    "/api/v1/users/{userId}": ['get'],
    "/api/v1/options": ['get'],
    "/api/v1/options/moods": ['get'],
    "/api/v1/users/{userId}/follow": ['post', 'delete'],
    "/api/v1/users/{userId}/followers": ['get'],
    "/api/v1/users/{userId}/following": ['get'],
    "/api/v1/users/{userId}/follow/status": ['get'],
    "/api/v1/vibes/sessions": ['post', 'get'],
    "/api/v1/vibes/sessions/{sessionId}/prompt": ['post'],
    "/api/v1/vibes/sessions/{sessionId}/result": ['get'],
    "/api/v1/vibes/results/{resultId}/items": ['get'],
    "/api/v1/vibes/items/{vibeItemId}/like": ['post'],
    "/api/v1/vibes/history": ['get'],
    "/api/v1/vibes/sessions/{sessionId}": ['get'],
    "/api/v1/users/me/password": ['put'],
    "/api/v1/users/me/social/{provider}": ['post', 'delete'],
    "/api/v1/users/me/social": ['get'],
    "/api/v1/users/me/nickname": ['put']
}

base_url = "http://localhost:8080"
headers = {"Content-Type": "application/json", "Accept": "application/json", "Accept-Language": "ko"}
token = None # We will try to get a token if login succeeds

def get_ref_schema(ref, components):
    if not ref:
        return {}
    parts = ref.split('/')
    if len(parts) == 4 and parts[1] == 'components' and parts[2] == 'schemas':
        return components.get('schemas', {}).get(parts[3], {})
    return {}

def generate_mock_data(schema, components, level=0):
    if level > 5:
        return None
    if '$ref' in schema:
        schema = get_ref_schema(schema['$ref'], components)
    
    type_ = schema.get('type')
    if type_ == 'string':
        if 'enum' in schema:
            return schema['enum'][0]
        if schema.get('format') == 'email':
            return 'test@example.com'
        if schema.get('format') == 'date-time':
            return '2025-01-01T00:00:00Z'
        return 'test_string'
    elif type_ == 'integer' or type_ == 'number':
        return 1
    elif type_ == 'boolean':
        return True
    elif type_ == 'array':
        item_schema = schema.get('items', {})
        return [generate_mock_data(item_schema, components, level+1)]
    elif type_ == 'object' or 'properties' in schema:
        props = schema.get('properties', {})
        return {k: generate_mock_data(v, components, level+1) for k, v in props.items()}
    return None

import builtins
output_file = open("/Users/bangseong-il/Documents/VibeConnector/Backend/test_0303.md", "w", encoding='utf-8')
output_file.write("# Developer A API 테스트 결과 (2026-03-03)\n\n")

for path, methods in target_endpoints.items():
    if path not in api_docs['paths']:
        output_file.write(f"## {path}\n")
        output_file.write(f"**Error:** Swagger 스펙에 경로가 존재하지 않습니다. 아직 미구현일 가능성이 높습니다.\n\n")
        continue

    for method in methods:
        method = method.lower()
        if method not in api_docs['paths'][path]:
            output_file.write(f"## {method.upper()} {path}\n")
            output_file.write(f"**Error:** Swagger 스펙에 해당 Method가 존재하지 않습니다.\n\n")
            continue

        operation = api_docs['paths'][path][method]
        summary = operation.get('summary', 'No summary')
        
        # Prepare Request URL
        actual_path = path
        # Replace path variables with dummy values
        if '{provider}' in actual_path:
            actual_path = actual_path.replace('{provider}', 'KAKAO')
        if '{userId}' in actual_path:
            actual_path = actual_path.replace('{userId}', '1')
        if '{sessionId}' in actual_path:
            actual_path = actual_path.replace('{sessionId}', '1')
        if '{resultId}' in actual_path:
            actual_path = actual_path.replace('{resultId}', '1')
        if '{vibeItemId}' in actual_path:
            actual_path = actual_path.replace('{vibeItemId}', '1')

        # query params
        query_params = {}
        if 'parameters' in operation:
            for p in operation['parameters']:
                if p.get('in') == 'query' and p.get('required'):
                    query_params[p['name']] = 'test_value' # simplified

        # body
        req_body = None
        if 'requestBody' in operation:
            content = operation['requestBody'].get('content', {})
            if 'application/json' in content:
                schema = content['application/json'].get('schema', {})
                req_body = generate_mock_data(schema, api_docs.get('components', {}))

        # Expected Response
        expected_resp = {}
        responses = operation.get('responses', {})
        # try 200 or 201
        success_code = next((c for c in ['200', '201'] if c in responses), None)
        if success_code:
            resp_content = responses[success_code].get('content', {})
            if '*/*' in resp_content:
                schema = resp_content['*/*'].get('schema', {})
                expected_resp = generate_mock_data(schema, api_docs.get('components', {}))
            elif 'application/json' in resp_content:
                schema = resp_content['application/json'].get('schema', {})
                expected_resp = generate_mock_data(schema, api_docs.get('components', {}))
        
        # Build curl command for documentation
        curl_cmd = f"curl -X {method.upper()} '{base_url}{actual_path}'"
        if req_body:
            curl_cmd += f" -H 'Content-Type: application/json' -d '{json.dumps(req_body)}'"
        if token:
            curl_cmd += f" -H 'Authorization: Bearer {token}'"
        
        # Execute Request
        req_headers = headers.copy()
        if token:
            req_headers['Authorization'] = f"Bearer {token}"
            
        try:
            url = f"{base_url}{actual_path}"
            if method == 'get':
                res = requests.get(url, headers=req_headers, params=query_params)
            elif method == 'post':
                res = requests.post(url, headers=req_headers, json=req_body, params=query_params)
            elif method == 'put':
                res = requests.put(url, headers=req_headers, json=req_body, params=query_params)
            elif method == 'delete':
                res = requests.delete(url, headers=req_headers, params=query_params)
            else:
                res = None

            actual_status = res.status_code if res else "N/A"
            try:
                actual_body = res.json()
            except:
                actual_body = res.text
            
            # If login, intercept token
            if path == "/api/v1/auth/login" and actual_status == 200:
                if isinstance(actual_body, dict):
                    data = actual_body.get('data', {})
                    if data and isinstance(data, dict):
                        token = data.get('accessToken')
            elif path == "/api/v1/auth/signup" and actual_status == 200:
                pass

        except Exception as e:
            actual_status = "Error"
            actual_body = str(e)
            
        is_error = actual_status >= 400 if isinstance(actual_status, int) else True
            
        output_file.write(f"## {method.upper()} {path} - {summary}\n\n")
        output_file.write(f"### 1. 실행 명령어\n```bash\n{curl_cmd}\n```\n\n")
        output_file.write(f"### 2. 실행후 예상 답변\n```json\n{json.dumps(expected_resp, ensure_ascii=False, indent=2)}\n```\n\n")
        output_file.write(f"### 3. 실행 후 실제 답변\n**Status Code**: {actual_status}\n```json\n{json.dumps(actual_body, ensure_ascii=False, indent=2) if isinstance(actual_body, dict) else actual_body}\n```\n\n")
        output_file.write(f"### 4. 에러 여부\n**{'Fail (Error)' if is_error else 'Success'}**\n\n")
        output_file.write("---\n\n")

output_file.close()
print("Done writing to test_0303.md")
