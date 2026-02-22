# Script de test pour le serveur HTTP

function Send-HttpRequest {
    param(
        [string]$Request,
        [string]$TestName,
        [int]$Port = 5000
    )
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "TEST: $TestName" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Requête envoyée:" -ForegroundColor Green
    Write-Host $Request -ForegroundColor Gray
    
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $client.Connect("localhost", $Port)
        $stream = $client.GetStream()
        $writer = New-Object System.IO.StreamWriter($stream)
        $reader = New-Object System.IO.StreamReader($stream)
        
        # Envoyer la requête
        $writer.Write($Request)
        $writer.Flush()
        
        # Lire la réponse
        $response = ""
        $lineCount = 0
        $inBody = $false
        $bodyLines = 0
        
        while ($lineCount -lt 100) {
            $line = $reader.ReadLine()
            if ($line -eq $null) { break }
            
            if (-not $inBody) {
                $response += $line + "`n"
                if ($line -eq "") {
                    $inBody = $true
                }
            } else {
                $bodyLines++
                if ($bodyLines -le 10) {
                    $response += $line + "`n"
                }
            }
            
            $lineCount++
            if ($inBody -and $bodyLines -ge 10) { break }
        }
        
        Write-Host "`nRéponse reçue:" -ForegroundColor Green
        Write-Host $response -ForegroundColor White
        
        $reader.Close()
        $writer.Close()
        $stream.Close()
        $client.Close()
        
        return $response
    }
    catch {
        Write-Host "ERREUR: $_" -ForegroundColor Red
        return $null
    }
}

Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║          TESTS EXERCICE 6 - Gestion des chemins           ║" -ForegroundColor Magenta
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Magenta

# Test 1: / → index.html
Send-HttpRequest -TestName "Test 1: / → index.html" -Request "GET / HTTP/1.1`r`nHost: site1`r`n`r`n"

# Test 2: Sous-dossier
Send-HttpRequest -TestName "Test 2: Sous-dossier /services/web.html" -Request "GET /services/web.html HTTP/1.1`r`nHost: site1`r`n`r`n"

# Test 3: Query ignorée
Send-HttpRequest -TestName "Test 3: Query ignorée /about.html?x=1" -Request "GET /about.html?x=1 HTTP/1.1`r`nHost: site1`r`n`r`n"

# Test 4: Décodage %20 (on teste avec un fichier qui existe, sinon on peut créer un fichier test)
Send-HttpRequest -TestName "Test 4: Décodage %20" -Request "GET /hello%20world.html HTTP/1.1`r`nHost: site1`r`n`r`n"

# Test 5: Dossier → index.html
Send-HttpRequest -TestName "Test 5: Dossier /services/ → index.html" -Request "GET /services/ HTTP/1.1`r`nHost: site1`r`n`r`n"

# Test 6: Fichier absent → 404
Send-HttpRequest -TestName "Test 6: Fichier absent → 404" -Request "GET /nope.html HTTP/1.1`r`nHost: site1`r`n`r`n"

# Test 7: Tentative de sortir du dossier → 400
Send-HttpRequest -TestName "Test 7: Path traversal /../tomuss.html → 400" -Request "GET /../tomuss.html HTTP/1.1`r`nHost: site1`r`n`r`n"

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║         TESTS EXERCICE 7 - Virtual hosting                ║" -ForegroundColor Magenta
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Magenta

# Test 8: Site 1
Send-HttpRequest -TestName "Test 8: Host: site1 → site1/" -Request "GET / HTTP/1.1`r`nHost: site1`r`n`r`n"

# Test 9: Site 2
Send-HttpRequest -TestName "Test 9: Host: site2 → site2/" -Request "GET / HTTP/1.1`r`nHost: site2`r`n`r`n"

# Test 10: Host inconnu → erreur
Send-HttpRequest -TestName "Test 10: Host inconnu → 404" -Request "GET / HTTP/1.1`r`nHost: inconnu`r`n`r`n"

# Test 11: Host avec port
Send-HttpRequest -TestName "Test 11: Host avec port localhost:5000" -Request "GET / HTTP/1.1`r`nHost: localhost:5000`r`n`r`n"

# Test 12: URL complète (forme proxy)
Send-HttpRequest -TestName "Test 12: URL complète (forme proxy)" -Request "GET http://site1/index.html HTTP/1.1`r`nHost: site1`r`n`r`n"

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║                   TESTS TERMINÉS                           ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green
