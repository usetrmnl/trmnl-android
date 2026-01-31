# TRMNL API Specification

This directory contains the OpenAPI specification for the TRMNL API.

## Resources

- **OpenAPI Portal:** https://trmnl.com/api-docs/index.html
- **API Documentation:** https://docs.trmnl.com/go
- **Private API Docs:** https://docs.trmnl.com/go/private-api/introduction

## Files

- `trmnl-openapi.yaml` - OpenAPI 3.0.1 specification exported from the TRMNL API portal

## Authentication

The TRMNL API uses two authentication methods:

1. **Device-level authentication** - Uses `Access-Token` header with device API key
   - Implemented in `TrmnlApiService.kt`
   - Used for device operations (display, setup, etc.)

2. **User-level authentication** - Uses `Authorization: Bearer` header with account API key
   - Implemented in `TrmnlUserApiService.kt`
   - Used for account operations (device management, plugin settings, etc.)
