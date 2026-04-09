# Supabase MCP Authentication Guide

## Step 1: Get Your Supabase Access Token

### Option A: From Supabase Dashboard (Recommended)

1. **Open Supabase Dashboard**: https://supabase.com/dashboard
2. **Login with your account** (the same one you want to use)
3. **Navigate to Account Settings**:
   - Click on your avatar/profile picture in the top right
   - Select "Account" or "Settings"
4. **Generate Access Token**:
   - Look for "Access Tokens" or "API Keys" section
   - Click "Generate New Token" or "Create Token"
   - Give it a descriptive name (e.g., "Windsurf MCP")
   - Select appropriate permissions (read/write access)
   - Copy the generated token

### Option B: Via CLI

If you have Supabase CLI installed:
```bash
supabase login
supabase projects list
supabase access-tokens create
```

## Step 2: Configure MCP Server

Once you have the access token, update the MCP configuration:

### Update the MCP settings file:
File: `C:\Users\WIN 10\AppData\Roaming\Windsurf\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json`

```json
{
  "mcpServers": {
    "supabase": {
      "command": "C:\\Program Files\\nodejs\\node.exe",
      "args": ["C:\\Users\\WIN 10\\AppData\\Roaming\\npm\\node_modules\\@supabase\\mcp-server-supabase\\dist\\transports\\stdio.js"],
      "env": {
        "SUPABASE_ACCESS_TOKEN": "YOUR_ACCESS_TOKEN_HERE"
      },
      "disabled": false
    }
  }
}
```

## Step 3: Restart Windsurf

After updating the configuration:
1. Close Windsurf IDE
2. Restart Windsurf IDE
3. The MCP server should automatically connect

## Step 4: Test Connection

You can test the connection by asking:
- "List my Supabase projects"
- "Show database schema for project [project-ref]"
- "List tables in my database"

## Additional Options

### Project-Specific Connection
If you want to connect to a specific project:

```json
{
  "mcpServers": {
    "supabase": {
      "command": "C:\\Program Files\\nodejs\\node.exe",
      "args": [
        "C:\\Users\\WIN 10\\AppData\\Roaming\\npm\\node_modules\\@supabase\\mcp-server-supabase\\dist\\transports\\stdio.js",
        "--project-ref",
        "your-project-ref"
      ],
      "env": {
        "SUPABASE_ACCESS_TOKEN": "YOUR_ACCESS_TOKEN_HERE"
      },
      "disabled": false
    }
  }
}
```

### Read-Only Mode
For safer operations:

```json
{
  "mcpServers": {
    "supabase": {
      "command": "C:\\Program Files\\nodejs\\node.exe",
      "args": [
        "C:\\Users\\WIN 10\\AppData\\Roaming\\npm\\node_modules\\@supabase\\mcp-server-supabase\\dist\\transports\\stdio.js",
        "--read-only"
      ],
      "env": {
        "SUPABASE_ACCESS_TOKEN": "YOUR_ACCESS_TOKEN_HERE"
      },
      "disabled": false
    }
  }
}
```

## Security Notes

- Keep your access token secure and never commit it to version control
- Use read-only mode when possible
- Generate project-specific tokens when possible
- Regularly rotate your access tokens

## Troubleshooting

### Common Issues:

1. **"npx not found"**: Node.js not in PATH (we fixed this)
2. **"Access token required"**: Token not set correctly
3. **"Permission denied"**: Token doesn't have required permissions
4. **"Project not found"**: Wrong project reference

### Debug Commands:

Test the MCP server directly:
```bash
"C:\Program Files\nodejs\node.exe" "C:\Users\WIN 10\AppData\Roaming\npm\node_modules\@supabase\mcp-server-supabase\dist\transports\stdio.js" --access-token YOUR_TOKEN
```

## Current Status

✅ Node.js installed and working
✅ Supabase MCP server installed
✅ MCP configuration file created
🔄 **Next step**: Get access token and update configuration
