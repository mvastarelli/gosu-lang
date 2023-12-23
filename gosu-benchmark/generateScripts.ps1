[CmdletBinding()]
param(
    [Parameter(Mandatory=$true, Position=0)][string]$ProjectPath,
    [Parameter(Mandatory=$true, Position=1)][string]$DestinationPath
)

function doTask($taskName, $task) {
    $startTime = Get-Date
    &$task
    $endTime = Get-Date
    $duration = $endTime - $startTime
    Write-Host "[$taskName] finished in [$duration]."
}

function createCache() {
    $path = Join-Path $env:TEMP (New-Guid)
    Write-Verbose "Creating cache directory at [$path]."
    New-Item -ItemType Directory -Path $path -Force
}

function removeCache($path) {
    if(Test-Path $path) {
        Write-Verbose "Removing cache directory at [$path]."
        Remove-Item -Path $path -Recurse -Force | Out-Null
    }
}

function collectFiles($path, $excludeList) {
    Get-ChildItem -Path $path -Filter *.gs -Recurse
    # Get-ChildItem -Path $path -Filter *.gsx -Recurse
}

function filterFiles($path, $files)
{
    $files |
            Where-Object { $_.DirectoryName -notmatch 'target' } |
            Where-Object { $_.Name -notmatch '^.+Test\.gs$' } |
            # Where-Object { $_.Name -notmatch '^Errant_.+\.gs$' } |
            Where-Object { $_.Name -notin $excludeList } |
            ForEach-Object {
                $packageLine = Get-Content -Path $_.FullName | Where-Object {
                    $_ -match '^package.+$'
                } | Select-Object -First 1

                if($null -eq $packageLine) {
                    Write-Verbose "Skipping file [$($_.FullName)] because it does not contain a package declaration."
                    return
                }

                $packageName = ($packageLine -replace '^package\s+', '').Trim()
                $packagePath = $packageName -replace '\.', [System.IO.Path]::DirectorySeparatorChar

                if ($packageName -match '\s+')
                {
                    Write-Verbose "Skipping file [$($_.FullName)] because it contains a space in the package declaration."
                    return
                }

                [PSCustomObject]@{
                    Path = Join-Path $path $packagePath
                    File = $_
                }
            }
}

function buildTree($files) {
    $files |
            Select-Object -ExpandProperty Path |
            Sort-Object |
            Get-Unique |
            ForEach-Object {
                Write-Verbose "Creating temporary directory [$_]."
                New-Item -ItemType Directory -Path $_ -Force | Out-Null
            }
}

function copyFiles($files) {
    $files |
            ForEach-Object {
                $targetFile = Join-Path $_.Path $_.File.Name
                Write-Verbose "Copying file [$($_.File.FullName)] to [$targetFile]."
                Copy-Item -Path $_.File.FullName -Destination $targetFile -Force | Out-Null
            }
}

function createArchive($path, $destination) {
    Add-Type -Assembly "System.IO.Compression.FileSystem"
    $archivePath = Join-Path (Resolve-Path -Path $destination) 'gosu-benchmark.zip'

    if(Test-Path -Path $archivePath) {
        Write-Verbose "Removing existing archive [$archivePath]."
        Remove-Item -Path $archivePath -Force | Out-Null
    }

    Write-Verbose "Creating archive [$archivePath] from [$path]."
    [System.IO.Compression.ZipFile]::CreateFromDirectory($path, $archivePath, 'Fastest', $false)
}

# -- Main Script --

$excludeList = @(
    'MyPogo.gs',
    'TestGosuClass.gs',
    'GSClassDocImpl.gs',
    'GSConstructorDocImpl.gs',
    'GSExecutableMemberDocImpl.gs',
    'GSMethodDocImpl.gs',
    'GSRootDocImpl.gs',
    'GSDocImpl.gs',
    'Errant_FLEnhancementMethods.gs',
    'Errant_FLCollections.gs'
)

Write-Host "Generating scripts for [$ProjectPath] and saving to [$DestinationPath]."

$cache = doTask 'Create cache' { createCache }
$allFiles = doTask 'Collect files' { collectFiles $ProjectPath $excludeList }
$filteredFiles = doTask 'Filter files' { filterFiles $cache.FullName $allFiles }

doTask 'Build tree' { buildTree $filteredFiles }
doTask 'Copy files' { copyFiles $filteredFiles }
doTask 'Create archive' { createArchive $cache.FullName $DestinationPath }
doTask 'Remove cache' { removeCache $cache.FullName }

Write-Host "Finished generating [$($filteredFiles.count)] scripts."