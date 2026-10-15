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
  window.smartvestSetLocation = function (latitude, longitude, color) {
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude) ||
        latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return;
    point = [latitude, longitude];
    if (!map) {
      map = L.map('map', {zoomControl: true, scrollWheelZoom: false}).setView(point, 16);
      tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'
      });
      tiles.on('loading', pending);
      tiles.on('tileload', function () { if (!tileFailed) loaded(); });
      tiles.on('tileerror', function () {
        tileFailed = true;
        clearTimeout(waiting);
        notice.textContent = 'No se pudo cargar parte del mapa. La ubicación sigue disponible.';
        retry.hidden = false;
      });
      tiles.addTo(map);
      map.on('dragstart', function () { follow = false; });
      marker = L.circleMarker(point, {radius: 9, color: '#fff', weight: 3, fillOpacity: 1}).addTo(map);
      pending();
    }
    marker.setLatLng(point).setStyle({fillColor: /^#[0-9a-f]{6}$/i.test(color) ? color : '#1E40AF'});
    if (follow) map.panTo(point, {animate: false});
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
  window.addEventListener('pagehide', function () { clearTimeout(waiting); });
})();
