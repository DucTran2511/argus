# CORS Configuration Guide

## Overview

This document explains the CORS (Cross-Origin Resource Sharing) configuration setup for the Argus backend API.

## What is CORS?

CORS is a security mechanism that allows or restricts web applications running at one origin (domain) to access resources from a different origin. Without proper CORS configuration, browsers will block requests from your frontend to the backend API.

## Configuration Files

### Java Configuration
- **File**: [`CorsConfig.java`](file:///mnt/apps/argus/src/main/java/com/argus/config/CorsConfig.java)
- Implements global CORS settings using Spring's `WebMvcConfigurer`
- Supports environment-specific configuration via properties

### Properties Files
1. **Development**: [`application.properties`](file:///mnt/apps/argus/src/main/resources/application.properties)
   - Allows `localhost:3000` (React) and `localhost:5173` (Vite)
   - Permissive settings for easier development

2. **Production**: [`application-prod.properties`](file:///mnt/apps/argus/src/main/resources/application-prod.properties)
   - Requires explicit `FRONTEND_URL` environment variable
   - Restricted headers for enhanced security

## Configuration Parameters

| Parameter | Description | Development | Production |
|-----------|-------------|-------------|------------|
| **allowed-origins** | Domains that can access the API | `localhost:3000,5173` | `${FRONTEND_URL}` |
| **allowed-methods** | HTTP methods permitted | `GET,POST,PUT,DELETE,PATCH,OPTIONS` | Same |
| **allowed-headers** | Request headers allowed | `*` (all) | Specific headers only |
| **exposed-headers** | Response headers visible to client | Standard set | Standard set |
| **allow-credentials** | Allow cookies/auth headers | `true` | `true` |
| **max-age** | Preflight cache duration (seconds) | `3600` | `3600` |

## Security Best Practices

### ✅ What We Implemented

1. **Explicit Origins**: No wildcards (`*`) in production
   ```properties
   # ❌ NEVER do this in production
   cors.allowed-origins=*
   
   # ✅ Always specify exact domains
   cors.allowed-origins=https://dashboard.argus.com
   ```

2. **Limited Methods**: Only necessary HTTP methods
   - Prevents attackers from using dangerous methods like `TRACE`

3. **Controlled Headers**: 
   - Development: `*` for convenience
   - Production: Specific headers only

4. **Credentials Handling**: Properly configured for authentication
   - Allows cookies and Authorization headers
   - Required for session-based or token-based auth

5. **Preflight Caching**: 1-hour cache to reduce OPTIONS requests

### 🔒 Production Deployment

When deploying to production, set the `FRONTEND_URL` environment variable:

```bash
# Docker
docker run -e FRONTEND_URL=https://dashboard.argus.com ...

# Kubernetes
env:
  - name: FRONTEND_URL
    value: "https://dashboard.argus.com"

# Direct Java
java -jar app.jar -DFRONTEND_URL=https://dashboard.argus.com
```

For multiple frontend domains:
```properties
cors.allowed-origins=https://dashboard.argus.com,https://app.argus.com
```

## Testing CORS

### Browser DevTools
1. Open Network tab
2. Make API request from frontend
3. Check for CORS errors:
   ```
   Access to fetch at 'http://localhost:8080/api/...' from origin 'http://localhost:3000'
   has been blocked by CORS policy
   ```

### Preflight Requests
Look for OPTIONS requests before actual requests:
```http
OPTIONS /api/wallets HTTP/1.1
Access-Control-Request-Method: POST
Access-Control-Request-Headers: content-type,authorization
```

Response should include:
```http
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

## Common Issues & Solutions

### Issue 1: "No 'Access-Control-Allow-Origin' header"
**Cause**: Origin not in allowed list  
**Solution**: Add origin to `cors.allowed-origins`

### Issue 2: "Credentials flag is 'true', but 'Access-Control-Allow-Credentials' header is ''"
**Cause**: `allowCredentials` not set  
**Solution**: Ensure `cors.allow-credentials=true`

### Issue 3: Preflight failing with custom headers
**Cause**: Custom headers not in allowed list  
**Solution**: Add headers to `cors.allowed-headers`

### Issue 4: Cannot use wildcard with credentials
**Cause**: Browser security restriction  
**Solution**: Replace `*` with explicit origins when using credentials

## Alternative Configuration

The `CorsConfig.java` includes a commented-out bean method for Spring Security integration:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    // ... configuration
}
```

If you're using Spring Security, uncomment this bean and remove the `WebMvcConfigurer` approach to let Security handle CORS.

## Monitoring

Watch for these in logs:
```
# Successful CORS
DEBUG c.a.config.CorsConfig - Adding CORS mapping for /**

# Failed CORS (if you enable debug logging)
WARN  o.s.web.cors.DefaultCorsProcessor - Rejected CORS request from origin 'https://evil.com'
```

## References

- [Spring CORS Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html)
- [MDN CORS Guide](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [OWASP CORS Best Practices](https://cheatsheetseries.owasp.org/cheatsheets/CORS_Cheat_Sheet.html)
