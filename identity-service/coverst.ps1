$secret = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437"
$bytes = [Convert]::FromBase64String($secret)
$base64url = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
Write-Host "Base64URL secret: $base64url"