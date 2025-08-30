param(
    [Parameter(Mandatory = $false, Position = 0)]
    [string]$Root = ".\input",

    [Parameter(Mandatory = $false, Position = 1)]
    [string]$Destination = ".\output"
)

# Requires exiftool.exe in PATH or same folder as script
function Get-MonthYear {
    param([string]$FilePath, [string]$exiftool)

    # Get the dates
    $dates = & $exiftool -DateTimeOriginal -CreateDate -d "%m/%Y" -s3 $FilePath

    # $dates is an array: first element is DateTimeOriginal, second is CreateDate
    $dateToUse = if ($dates -and $dates.Count -ge 1 -and $dates[0] -match '^(0[1-9]|1[0-2])/(\d{4})$') { 
        Write-Host "Using DateTimeOriginal"
        $dates[0]   # Use DateTimeOriginal if valid
    } elseif ($dates -and $dates.Count -ge 2 -and $dates[1] -match '^(0[1-9]|1[0-2])/(\d{4})$') {
        Write-Host "Using CreationDate"
        $dates[1]   # Fallback to CreateDate
    } else {
        Write-Host "Using fallback LastWriteTime"
        (Get-Item $FilePath).LastWriteTime.ToString("MM/yyyy")  # note that CreationTime gets overwritten when copying
    }

    return $dateToUse 
}

function Get-MonthYearEarliest {
    param([string]$FilePath, [string]$exiftool)

    $validTimes = @()
    $times = & $exiftool -time:all -s3 $FilePath
    if ($times) {
      $validTimes = $times | Where-Object { $_ -match '^\d{4}:\d{2}:\d{2}' }
    }

    if ($validTimes -gt 0) {
      Write-Host "Extracted earliest time from EXIF"
      $earliest = $validTimes | Sort-Object | Select-Object -First 1
    }
    else {
      Write-Host "Using fallback LastWriteTime"
      $earliest = (Get-Item $FilePath).LastWriteTime  # note that CreationTime gets overwritten when copying
    }

    if ($earliest.Length -ge 19) {
      $dt = [datetime]::ParseExact($earliest.Substring(0,19), "yyyy:MM:dd HH:mm:ss", $null)
      $dt.ToString("MM/yyyy")
    }
    else {
      Write-Host "Can't parse: $earliest"
    }
}

Start-Transcript -Path ".\move.output" -Append

if (-not (Test-Path $Root)) {
    Write-Host "Input folder does not exist: $Root"
    exit 1
}
elseif (-not (Test-Path $Destination)) {
    Write-Host "Creating destination folder: $Destination"
    New-Item -ErrorAction Stop -ItemType Directory -Path $Destination | Out-Null
}


# Ensure it's a full path
$root = Resolve-Path $Root
$dest = Resolve-Path $Destination

# Define the path to the executable
$exiftool = ".\exiftool\exiftool.exe"

# Check if the executable exists
if (Test-Path $exiftool) {
    Write-Host "exiftool found at: $exiftool"
} else {
    Write-Host "exiftool not found"
}

Write-Host "root: $root"
Write-Host "dest: $dest"

Get-ChildItem -Path $root -Recurse -File | ForEach-Object {
    $file = $_
    Write-Host "Working with: $file"
    $ext = $file.Extension.ToLower()

    $isImage = $ext -in ".jpg",".jpeg",".png",".gif"
    $isVideo = $ext -in ".mp4",".3gp",".avi",".mpeg",".mpg"

    if ($isImage -or $isVideo -or $file.Length -gt 2KB) {
        $monthYear = Get-MonthYearEarliest -FilePath $file.FullName -exiftool $exiftool
        $parts = $monthYear -split "/"
        $mm = $parts[0]
        $yyyy = $parts[1]

        $targetDir = Join-Path $dest "$yyyy\$mm"
        if (-not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir | Out-Null
        }

        # Build target path safely (avoid overwriting)
        $baseName = [System.IO.Path]::GetFileNameWithoutExtension($file.Name)
        $extension = $file.Extension
        $targetPath = Join-Path $targetDir $file.Name

        # Only rename if file already exists
        if (Test-Path $targetPath) {
            $counter = 1
            do {
                $suffix = ("_{0:D3}" -f $counter)  # _001, _002, ...
                $newName = "$baseName$suffix$extension"
                $targetPath = Join-Path $targetDir $newName
                $counter++
            } while (Test-Path $targetPath)
        }

        Move-Item -Path $file.FullName -Destination $targetPath -Force
        Write-Host "Moved $($file.Name) -> $targetPath"
    }
}

Stop-Transcript
