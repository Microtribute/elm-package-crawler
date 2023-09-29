$packages = Get-Content -Raw -Path "./all-packages.json" | ConvertFrom-Json

echo $packages
