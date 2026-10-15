// Runs inside the bundled map document. No account, device or person data is exposed here.
(function () {
  'use strict';
  var map, marker, tiles, point, follow = true, waiting, tileFailed = false;
  var notice = document.getElementById('map-status');
  var retry = document.getElementById('map-retry');
  var recenter = document.getElementById('map-center');
  function pending() {
    clearTimeout(waiting);
    waiting = setTimeout(function () {
      notice.textContent = 'El mapa tarda en cargar. Comprueba la conexión.';
      retry.hidden = false;
    }, 12000);
  }
  function loaded() {
    clearTimeout(waiting);
    notice.textContent = '';
    retry.hidden = true;
  }
  function ensureMap() {
    if (map) return;
    // A world overview is a viewport, never a reported vest position.
    map = L.map('map', {zoomControl: true, scrollWheelZoom: false}).setView([0, 0], 1);
    tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'
    });
    tiles.on('loading', pending);
    tiles.on('tileload', function () { if (!tileFailed) loaded(); });
    tiles.on('tileerror', function () {
      tileFailed = true;
      clearTimeout(waiting);
      notice.textContent = 'No se pudo cargar parte del mapa. Comprueba la conexión.';
      retry.hidden = false;
    });
    tiles.addTo(map);
    map.on('dragstart', function () { follow = false; });
    pending();
  }
  window.smartvestClearLocation = function () {
    ensureMap();
    if (marker) { marker.remove(); marker = null; }
    point = null;
    follow = true;
    recenter.disabled = true;
    document.getElementById('map').setAttribute('aria-label', 'Mapa general. Sin ubicación GPS del chaleco');
    map.setView([0, 0], 1, {animate: false});
  };
  window.smartvestSetLocation = function (latitude, longitude, color) {
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude) ||
        latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return;
    ensureMap();
    point = [latitude, longitude];
    var firstFix = !marker;
    if (firstFix) marker = L.circleMarker(point, {radius: 9, color: '#fff', weight: 3, fillOpacity: 1}).addTo(map);
    marker.setLatLng(point).setStyle({fillColor: /^#[0-9a-f]{6}$/i.test(color) ? color : '#1E40AF'});
    recenter.disabled = false;
    document.getElementById('map').setAttribute('aria-label', 'Mapa de la última ubicación recibida del chaleco');
    if (firstFix) map.setView(point, 16, {animate: false});
    else if (follow) map.panTo(point, {animate: false});
    map.invalidateSize({pan: false});
  };
  retry.addEventListener('click', function () {
    if (!tiles) return;
    tileFailed = false;
    notice.textContent = 'Cargando mapa…';
    retry.hidden = true;
    pending();
    tiles.redraw();
  });
  recenter.addEventListener('click', function () {
    if (map && point) { follow = true; map.setView(point, map.getZoom(), {animate: false}); }
  });
  window.addEventListener('resize', function () { if (map) map.invalidateSize({pan: false}); });
  if (window.ResizeObserver) new ResizeObserver(function () {
    if (map) map.invalidateSize({pan: false});
  }).observe(document.getElementById('map'));
  ensureMap();
  window.addEventListener('pagehide', function () { clearTimeout(waiting); });
})();
