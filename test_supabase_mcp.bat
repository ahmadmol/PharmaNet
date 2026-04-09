@echo off
set PATH=C:\Program Files\nodejs;%PATH%
echo Testing Supabase MCP connection...
echo.
echo Requesting tools list...
echo {"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}} | npx -y mcp-remote "https://mcp.supabase.com/mcp?project_ref=zispernfhcsbjnhymepc&read_only=true&features=database%2Cdevelopment%2Cfunctions%2Cbranching%2Cdebugging%2Caccount%2Cdocs"
echo.
echo Test complete!
pause
