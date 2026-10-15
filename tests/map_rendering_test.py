"""Regression test for the APK's bundled map engine. All map tiles are PRUEBA fixtures.
Run with Python + Playwright Chromium installed. Never contacts a real tile service.
"""
from pathlib import Path
import struct
import zlib
import re
from playwright.sync_api import sync_playwright

root = Path(__file__).resolve().parents[1]
assets = root / 'android-native/app/src/main/assets/maps'
source = (root / 'android-native/app/src/main/java/com/ajtecnology/smartvest/ui/LocationMapDocument.kt').read_text()
html = source.split('= """', 1)[1].split('""".trimIndent()', 1)[0]
html = html.replace('$css', (assets / 'leaflet.css').read_text()).replace('$leaflet', (assets / 'leaflet.js').read_text()).replace('$controller', (assets / 'location.js').read_text())
def chunk(kind, data):
    return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data) & 0xffffffff)
png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!IIBBBBB', 256, 256, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress((b'\x00' + b'\xe2\xe8\xf0' * 256) * 256)) + chunk(b'IEND', b'')

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 360, 'height': 280})
    errors = []
    requests = []
    page.on('pageerror', lambda error: errors.append(str(error)))
    page.on('request', lambda request: requests.append(request.url))
    page.route('https://tile.openstreetmap.org/**', lambda route: route.fulfill(status=200, content_type='image/png', body=png))
    hook = "L.Map.addInitHook(function () { window.PRUEBA_map = this; window.PRUEBA_instances = (window.PRUEBA_instances || 0) + 1; });"
    test_html = html.replace('<script>' + (assets / 'location.js').read_text(), '<script>' + hook + (assets / 'location.js').read_text())
    page.set_content(test_html, wait_until='networkidle')
    # The actual reported bug: the map must render before any GPS fix arrives.
    page.locator('#map .leaflet-tile-loaded').first.wait_for(state='attached')
    assert page.evaluate('PRUEBA_instances') == 1
    assert page.locator('.leaflet-interactive').count() == 0
    assert page.locator('#map-center').is_disabled()
    assert page.locator('#map').bounding_box()['height'] >= 200
    page.evaluate("smartvestSetLocation(0.123456,-79.123456,'#1E40AF')")
    page.wait_for_load_state('networkidle')
    page.locator('#map .leaflet-tile-loaded').first.wait_for(state='attached')
    assert page.locator('#map .leaflet-tile-loaded').count() > 0
    assert page.locator('#map-retry').is_hidden()
    assert page.locator('a[href="https://www.openstreetmap.org/copyright"]').is_visible()
    # Poll updates move the same marker/map, preserving the user's zoom and pan.
    page.evaluate("() => { PRUEBA_map.setZoom(15, {animate:false}); PRUEBA_map.fire('dragstart'); PRUEBA_map.panTo([0.123,-79.124], {animate:false}); }")
    before = page.evaluate('PRUEBA_map.getCenter()')
    for i in range(20):
        page.evaluate(f"smartvestSetLocation({0.123456 + i * .00001},-79.123456,'#B91C1C')")
    assert page.evaluate('PRUEBA_instances') == 1
    assert page.evaluate('PRUEBA_map.getZoom()') == 15
    after = page.evaluate('PRUEBA_map.getCenter()')
    assert abs(after['lat'] - before['lat']) < .000001 and abs(after['lng'] - before['lng']) < .000001, (before, after)
    assert page.locator('.leaflet-interactive').count() == 1
    page.locator('#map-center').click()
    assert page.evaluate('PRUEBA_map.getCenter()') != before
    # Failure remains visible and the user can retry without remounting the map.
    failed_once = {'done': False}
    def partially_failed_tiles(route):
        if not failed_once['done']:
            failed_once['done'] = True
            route.abort()
        else:
            route.fulfill(status=200, content_type='image/png', body=png)
    page.route('https://tile.openstreetmap.org/**', partially_failed_tiles)
    page.evaluate("PRUEBA_map.eachLayer(function (layer) { if (layer instanceof L.TileLayer) layer.redraw(); });")
    page.wait_for_function("!document.getElementById('map-retry').hidden")
    page.wait_for_load_state('networkidle')
    assert page.locator('#map-retry').is_visible()
    assert 'No se pudo' in page.locator('#map-status').inner_text()
    page.route('https://tile.openstreetmap.org/**', lambda route: route.fulfill(status=200, content_type='image/png', body=png))
    page.locator('#map-retry').click()
    page.wait_for_function("document.getElementById('map-retry').hidden")
    assert page.evaluate('PRUEBA_instances') == 1
    # Portrait/compact viewport never collapses the map or hides attribution.
    page.set_viewport_size({'width': 320, 'height': 280})
    assert page.locator('#map').bounding_box()['height'] >= 200
    # A different device without GPS clears the pin and does not inherit the old position.
    page.evaluate('smartvestClearLocation()')
    assert page.locator('.leaflet-interactive').count() == 0
    assert page.locator('#map-center').is_disabled()
    assert page.evaluate('PRUEBA_instances') == 1
    page.evaluate("smartvestSetLocation(0.123456,-79.123456,'#1E40AF')")
    assert page.locator('.leaflet-interactive').count() == 1
    assert page.locator('#map-center').is_enabled()
    # Android can initially report an unmeasured viewport; keep a usable map height.
    page.set_viewport_size({'width': 360, 'height': 1})
    assert page.locator('#map').bounding_box()['height'] > 100
    assert not errors, errors
    assert all(re.match(r'https://tile\.openstreetmap\.org/', url) for url in requests), requests
    browser.close()
print('map_rendering_test: render, polling, pan/zoom, retry, attribution and compact size OK (PRUEBA tiles)')
