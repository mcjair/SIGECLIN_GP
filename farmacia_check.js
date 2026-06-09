
        function cargarRecetas() {
            fetch('/api/farmacia/recetas')
                .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
                .then(data => {
                    const tb = document.getElementById('recetasBody');
                    if (!data.length) {
                        tb.innerHTML = '<tr><td colspan="5" class="text-center py-5 text-muted"><i class="bi bi-capsule fs-1 d-block mb-2"></i>No hay recetas pendientes.</td></tr>';
                        return;
                    }
                    let html = '';
                    data.forEach(r => {
                        const itemsHtml = r.items.map(i =>
                            `<div class="d-flex justify-content-between align-items-center">
                                <span class="fw-800 text-ink small">${i.medicamento} ${i.concentracion || ''}</span>
                                <span class="badge ${i.estadoDispensacion === 'dispensado' ? 'bg-success text-success' : 'bg-warning bg-opacity-10 text-warning'} fw-800 extra-small">${i.estadoDispensacion.toUpperCase()}</span>
                            </div>`
                        ).join('');
                        html += `<tr class="hover-scale transition-all">
                            <td class="ps-4"><div class="fw-800 text-primary">REC-${String(r.idReceta).padStart(6,'0')}</div></td>
                            <td><div class="fw-800 text-ink text-uppercase small">${r.paciente}</div><div class="extra-small text-muted fw-bold">${r.pacienteDni}</div></td>
                            <td>${itemsHtml}</td>
                            <td class="small">${r.fecha ? new Date(r.fecha).toLocaleDateString('es-PE') : '--'}</td>
                            <td class="pe-4 text-center">
                                <div class="d-flex gap-2 justify-content-center">
                                <button class="btn btn-sm btn-outline-info rounded-pill px-3 fw-900 shadow-sm" onclick="abrirDetallesReceta(${r.idReceta})"><i class="bi bi-file-text me-1"></i> DETALLES</button>
                                <button class="btn btn-sm btn-indigo-grad rounded-pill px-3 fw-900 shadow-sm" onclick="abrirDispensar(${r.idReceta})"><i class="bi bi-capsule-pill me-1"></i> DISPENSAR</button>
                                </div>
                            </td>
                        </tr>`;
                    });
                    tb.innerHTML = html;
                });
        }

        function abrirDetallesReceta(idReceta) {
            const body = document.getElementById('modalDetallesBody');
            if (!body) return;
            body.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-primary"></div><p class="mt-2 text-muted">Cargando detalles...</p></div>';
            new bootstrap.Modal(document.getElementById('modalDetallesReceta')).show();

            fetch('/api/farmacia/recetas')
                .then(r => r.json())
                .then(recetas => {
                    const receta = recetas.find(r => r.idReceta === idReceta);
                    if (!receta) { body.innerHTML = '<div class="alert alert-danger">Receta no encontrada</div>'; return; }
                    const pendientes = receta.items.filter(i => i.estadoDispensacion !== 'dispensado');
                    const dispensados = receta.items.filter(i => i.estadoDispensacion === 'dispensado');
                    let html = `<div class="row g-4">
                        <div class="col-lg-3">
                            <div class="card border-0 bg-primary-soft p-4 text-center h-100 rounded-4">
                                <div class="avatar mx-auto mb-3 bg-primary text-white rounded-4 d-flex align-items-center justify-content-center fw-bold shadow-sm" style="width:80px;height:80px;font-size:1.8rem;">
                                    ${receta.paciente ? receta.paciente.charAt(0).toUpperCase() : 'P'}
                                </div>
                                <h5 class="fw-800 text-ink mb-1">${receta.paciente || '---'}</h5>
                                <div class="text-muted small fw-bold">${receta.pacienteDni || '---'}</div>
                                <hr class="my-3 opacity-25">
                                <div class="text-start small">
                                    <div class="d-flex justify-content-between mb-1"><span class="text-muted fw-bold">Receta:</span><span class="fw-800 text-primary">REC-${String(idReceta).padStart(6,'0')}</span></div>
                                    <div class="d-flex justify-content-between mb-1"><span class="text-muted fw-bold">EmisiÃ³n:</span><span class="fw-800">${receta.fecha ? new Date(receta.fecha).toLocaleDateString('es-PE') : '--'}</span></div>
                                    <div class="d-flex justify-content-between"><span class="text-muted fw-bold">Ãtems:</span><span class="fw-800">${receta.items.length} (${dispensados.length} dispensados)</span></div>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-5">
                            <h6 class="fw-800 text-ink mb-3"><i class="bi bi-capsule me-2 text-primary"></i>MEDICAMENTOS PRESCRITOS</h6>
                            <div class="d-flex flex-column gap-2">`;
                    receta.items.forEach(i => {
                        const isDone = i.estadoDispensacion === 'dispensado';
                        html += `<div class="card border ${isDone ? 'border-success bg-success-soft' : 'border-primary bg-white'} p-3 rounded-4">
                            <div class="d-flex justify-content-between align-items-center">
                                <div><h6 class="fw-800 text-ink mb-0">${i.medicamento} ${i.concentracion || ''}</h6>
                                <p class="small text-muted mb-0">${i.dosis} â€” C/${i.frecuencia} â€” ${i.duracion} dÃ­as (${i.cantidad} U)</p></div>
                                <span class="badge ${isDone ? 'bg-success' : 'bg-warning bg-opacity-10 text-warning'} fw-800 px-3 rounded-pill">${isDone ? 'DISPENSADO' : 'PENDIENTE'}</span>
                            </div>
                        </div>`;
                    });
                    html += `</div></div>
                        <div class="col-lg-4">
                            <div class="card border-0 bg-light p-4 rounded-4 h-100">
                                <h6 class="fw-800 text-ink mb-3"><i class="bi bi-clipboard-data me-2 text-primary"></i>RESUMEN</h6>
                                <div class="d-flex gap-3 flex-wrap mb-3">
                                    <div class="bg-white rounded-4 p-3 text-center flex-fill shadow-sm">
                                        <div class="fs-2 fw-800 text-primary">${receta.items.length}</div>
                                        <div class="extra-small text-muted fw-bold">TOTAL</div>
                                    </div>
                                    <div class="bg-white rounded-4 p-3 text-center flex-fill shadow-sm">
                                        <div class="fs-2 fw-800 text-success">${dispensados.length}</div>
                                        <div class="extra-small text-muted fw-bold">DISPENSADOS</div>
                                    </div>
                                    <div class="bg-white rounded-4 p-3 text-center flex-fill shadow-sm">
                                        <div class="fs-2 fw-800 text-warning">${pendientes.length}</div>
                                        <div class="extra-small text-muted fw-bold">PENDIENTES</div>
                                    </div>
                                </div>
                                <div class="mt-auto">
                                    <button class="btn btn-indigo-grad w-100 rounded-pill fw-800 shadow-sm" onclick="abrirDispensar(${idReceta});bootstrap.Modal.getInstance(document.getElementById('modalDetallesReceta')).hide();"><i class="bi bi-capsule-pill me-1"></i> IR A DISPENSAR</button>
                                </div>
                            </div>
                        </div>
                    </div>`;
                    body.innerHTML = html;
                })
                .catch(e => { console.error('Error cargando detalles receta:', e); body.innerHTML = '<div class="alert alert-danger">Error: ' + e.message + '</div>'; });
        }

        function abrirDispensar(idReceta) {
            const modalEl = document.getElementById('modalDispensar');
            const body = document.getElementById('modalDispensarBody');
            if (!modalEl || !body) { console.error('modalDispensar no encontrado'); return; }
            body.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-primary"></div><p class="mt-2 text-muted">Cargando detalles...</p></div>';
            new bootstrap.Modal(modalEl).show();

            fetch('/api/farmacia/recetas')
                .then(r => r.json())
                .then(recetas => {
                    const receta = recetas.find(r => r.idReceta === idReceta);
                    if (!receta) { body.innerHTML = '<div class="alert alert-danger">Receta no encontrada</div>'; return; }
                    const pendientes = receta.items.filter(i => i.estadoDispensacion !== 'dispensado');
                    let html = `<div class="mb-3"><h6 class="fw-800 text-ink">Receta: <span class="text-primary">REC-${String(idReceta).padStart(6,'0')}</span></h6>
                        <p class="small text-muted mb-0">${receta.paciente} â€” ${receta.pacienteDni}</p></div>`;
                    html += '<hr>';
                    pendientes.forEach(item => {
                        html += `<div class="card border p-3 mb-2 rounded-4" id="item-${item.idDetalle}">
                            <div class="d-flex justify-content-between align-items-start">
                                <div><h6 class="fw-800 text-ink mb-1">${item.medicamento} ${item.concentracion || ''}</h6>
                                    <p class="small text-muted mb-1">${item.dosis} â€” ${item.frecuencia} â€” ${item.duracion} dÃ­as â€” Cant: ${item.cantidad}</p>
                                    <div id="lotes-${item.idDetalle}" class="mt-2"><select class="form-select-premium form-select-sm" id="loteSel-${item.idDetalle}"><option>Cargando lotes...</option></select></div>
                                </div>
                                <div class="text-end">
                                    <input type="number" class="form-control-premium form-control-sm text-center" id="cant-${item.idDetalle}" value="${item.cantidad}" min="1" max="${item.cantidad}" style="width:70px">
                                    <button class="btn btn-sm btn-success rounded-pill mt-1 fw-800 w-100" onclick="confirmarDispensar(${item.idDetalle})"><i class="bi bi-check-lg"></i> DISPENSAR</button>
                                    <div id="result-${item.idDetalle}"></div>
                                </div>
                            </div>
                        </div>`;
                        cargarLotes(item.idDetalle, item.idMedicamento);
                    });
                    if (!pendientes.length) html += '<div class="alert alert-success">Todos los Ã­tems de esta receta han sido dispensados.</div>';
                    body.innerHTML = html;
                })
                .catch(e => { console.error('Error en abrirDispensar:', e); body.innerHTML = '<div class="alert alert-danger">Error: ' + e.message + '</div>'; });
        }

        function cargarLotes(idDetalle, idMedicamento) {
            fetch('/api/farmacia/lotes/' + idMedicamento)
                .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
                .then(lotes => {
                    const sel = document.getElementById('loteSel-' + idDetalle);
                    if (!sel) return;
                    if (!lotes.length) { sel.innerHTML = '<option value="">Sin stock disponible</option>'; return; }
                    sel.innerHTML = lotes.map(l =>
                        `<option value="${l.idLote}">${l.numeroLote} â€” Stock: ${l.stockActual} â€” Vence: ${new Date(l.fechaVencimiento).toLocaleDateString('es-PE')}</option>`
                    ).join('');
                });
        }

        function confirmarDispensar(idDetalle) {
            const sel = document.getElementById('loteSel-' + idDetalle);
            const cant = document.getElementById('cant-' + idDetalle);
            const resultDiv = document.getElementById('result-' + idDetalle);
            if (!sel || !sel.value) { resultDiv.innerHTML = '<span class="text-danger small fw-800">Seleccione un lote</span>'; return; }
            const body = {
                idDetalleReceta: idDetalle,
                idLote: parseInt(sel.value),
                cantidad: parseInt(cant.value),
                observaciones: ''
            };
            fetch('/api/farmacia/dispensar', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(body)
            })
            .then(r => { if (!r.ok) { return r.json().then(e => { throw new Error(e.error || 'Error ' + r.status); }); } return r.json(); })
            .then(res => {
                resultDiv.innerHTML = `<span class="text-success small fw-800">âœ” ${res.medicamento} â€” ${res.cantidad} und. Lote: ${res.lote} â€” Stock restante: ${res.stockRestante}</span>`;
                document.getElementById('item-' + idDetalle).classList.add('bg-success-soft');
                cargarRecetas();
                cargarAlertas();
                cargarStock();
                cargarHistorial();
            })
            .catch(e => resultDiv.innerHTML = `<span class="text-danger small fw-800">${e.message}</span>`);
        }

        function cargarAlertas() {
            fetch('/api/farmacia/alertas')
                .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
                .then(data => {
                    const panel = document.getElementById('alertasPanel');
                    document.getElementById('alertaCount').textContent = data.length;
                    if (!data.length) { panel.innerHTML = ''; return; }
                    let html = '';
                    data.forEach(a => {
                        const isBajo = a.tipo === 'stock_bajo';
                        html += `<div class="col-md-6 col-lg-4">
                            <div class="card border-0 shadow-sm p-3 rounded-4 alerta-card ${isBajo ? 'border-start border-danger border-4' : 'border-start border-warning border-4'} bg-white">
                                <div class="d-flex align-items-center gap-2">
                                    <i class="bi ${isBajo ? 'bi-exclamation-triangle-fill text-danger' : 'bi-clock-fill text-warning'} fs-5"></i>
                                    <div><h6 class="fw-800 text-ink small mb-0">${isBajo ? 'Stock Bajo' : 'PrÃ³ximo a Vencer'}</h6>
                                    <p class="small text-muted mb-0">${a.medicamento} â€” Lote: ${a.lote}</p>
                                    <span class="badge ${isBajo ? 'bg-danger-soft text-danger' : 'bg-warning-soft text-warning'} fw-800">${isBajo ? 'Stock: '+a.stockActual+' (mÃ­n: '+a.stockMinimo+')' : 'Vence: '+new Date(a.fechaVencimiento).toLocaleDateString('es-PE')+' â€” Stock: '+a.stockActual}</span></div>
                                </div>
                            </div>
                        </div>`;
                    });
                    panel.innerHTML = html;
                });
        }

        function cargarStock() {
            fetch('/api/farmacia/stock')
                .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
                .then(data => {
                    const tb = document.getElementById('stockBody');
                    if (!data.length) { tb.innerHTML = '<tr><td colspan="5" class="text-center py-5 text-muted"><i class="bi bi-box fs-1 d-block mb-2"></i>Sin stock registrado.</td></tr>'; return; }
                    let html = '';
                    data.forEach(s => {
                        const stock = s.stockActual || 0;
                        const total = s.stockTotal || 0;
                        let stockClass = 'stock-ok', venClass = '';
                        let badge = '';
                        if (s.sinStock) {
                            stockClass = 'text-muted';
                            badge = '<span class="badge bg-secondary fw-800">SIN STOCK</span>';
                        } else if (s.vencido) {
                            stockClass = 'stock-critico'; venClass = 'vencido';
                            badge = '<span class="badge bg-danger fw-800">VENCIDO</span>';
                        } else if (total < 10) {
                            stockClass = 'stock-bajo';
                            badge = '<span class="badge bg-warning text-warning fw-800">STOCK BAJO</span>';
                        } else {
                            badge = '<span class="badge bg-success text-success fw-800">OK</span>';
                        }
                        let fechaHtml = '--';
                        if (s.fechaVencimiento) {
                            const f = new Date(s.fechaVencimiento);
                            const diasVence = Math.floor((f - new Date()) / (1000*60*60*24));
                            fechaHtml = f.toLocaleDateString('es-PE');
                            if (diasVence <= 60 && !s.vencido) fechaHtml += ` <span class="badge bg-warning-soft text-warning fw-800 ms-1">${diasVence}d</span>`;
                        }
                        html += `<tr class="${venClass}">
                            <td class="ps-4"><div class="fw-800 text-ink">${s.medicamento}</div><div class="extra-small text-muted">${s.presentacion || ''} ${s.concentracion || ''}</div></td>
                            <td class="fw-bold text-muted">${s.numeroLote || '--'}</td>
                            <td class="small">${fechaHtml}</td>
                            <td><span class="${stockClass} fs-5">${total}</span></td>
                            <td class="pe-4 text-center">${badge}</td>
                        </tr>`;
                    });
                    tb.innerHTML = html;
                });
        }

        function cargarHistorial() {
            fetch('/api/farmacia/historial')
                .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
                .then(data => {
                    const tb = document.getElementById('historialBody');
                    if (!data.length) { tb.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-muted"><i class="bi bi-clock-history fs-1 d-block mb-2"></i>Sin dispensaciones registradas.</td></tr>'; return; }
                    let html = '';
                    data.forEach(h => {
                        html += `<tr>
                            <td class="ps-4 small">${new Date(h.fecha).toLocaleString('es-PE')}</td>
                            <td class="fw-800 text-ink small">${h.paciente}</td>
                            <td class="fw-800 text-ink">${h.medicamento}</td>
                            <td class="fw-bold text-muted">${h.lote}</td>
                            <td class="fw-800 text-primary">${h.cantidad}</td>
                            <td class="small text-muted">${h.usuario}</td>
                        </tr>`;
                    });
                    tb.innerHTML = html;
                });
        }

        function abrirNuevoLote() {
            const f = document.getElementById('formNuevoLote');
            if (!f) return;
            f.reset();
            document.getElementById('loteMedId').value = '';
            document.getElementById('loteMedResults').classList.add('d-none');
            document.getElementById('loteResultMsg').innerHTML = '';
            new bootstrap.Modal(document.getElementById('modalNuevoLote')).show();
            setTimeout(() => { const s = document.getElementById('loteMedSearch'); if (s) s.focus(); }, 300);
        }

        function recargarFarmacia() { cargarRecetas(); cargarAlertas(); cargarStock(); cargarHistorial(); }

        document.addEventListener('DOMContentLoaded', function() {
            recargarFarmacia();

            var loteSearch = document.getElementById('loteMedSearch');
            if (loteSearch) {
                loteSearch.addEventListener('input', function() {
                    const q = this.value.trim();
                    const results = document.getElementById('loteMedResults');
                    if (q.length < 2) { results.classList.add('d-none'); return; }
                    fetch('/api/medicamentos/buscar?q=' + encodeURIComponent(q))
                        .then(r => r.json())
                        .then(data => {
                            if (!data.length) { results.innerHTML = '<a class="list-group-item list-group-item-action text-muted small">Sin resultados</a>'; results.classList.remove('d-none'); return; }
                            results.innerHTML = data.map(m =>
                                `<a class="list-group-item list-group-item-action small fw-800" href="#" data-id="${m.idMedicamento}" data-nom="${m.nombreGenerico} ${m.concentracion || ''}">${m.nombreGenerico} ${m.concentracion || ''} â€” ${m.presentacion || ''}</a>`
                            ).join('');
                            results.classList.remove('d-none');
                            results.querySelectorAll('a').forEach(a => {
                                a.addEventListener('click', function(e) {
                                    e.preventDefault();
                                    document.getElementById('loteMedSearch').value = this.dataset.nom;
                                    document.getElementById('loteMedId').value = this.dataset.id;
                                    results.classList.add('d-none');
                                });
                            });
                        });
                });
            }

            document.addEventListener('click', function(e) {
                if (!e.target.closest('#loteMedResults') && !e.target.closest('#loteMedSearch')) {
                    var r = document.getElementById('loteMedResults');
                    if (r) r.classList.add('d-none');
                }
            });

            var loteForm = document.getElementById('formNuevoLote');
            if (loteForm) {
                loteForm.addEventListener('submit', function(e) {
                    e.preventDefault();
                    const msgDiv = document.getElementById('loteResultMsg');
                    const btn = document.getElementById('btnGuardarLote');
                    const idMed = document.getElementById('loteMedId').value;
                    if (!idMed) { msgDiv.innerHTML = '<div class="alert alert-danger py-2 small fw-800">Seleccione un medicamento</div>'; return; }
                    const body = {
                        idMedicamento: parseInt(idMed),
                        numeroLote: document.getElementById('loteNumero').value.trim(),
                        fechaVencimiento: document.getElementById('loteFechaVenc').value,
                        cantidadInicial: parseInt(document.getElementById('loteCantidad').value)
                    };
                    if (!body.numeroLote || !body.fechaVencimiento || !body.cantidadInicial) {
                        msgDiv.innerHTML = '<div class="alert alert-danger py-2 small fw-800">Complete todos los campos</div>'; return;
                    }
                    btn.disabled = true;
                    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> GUARDANDO...';
                    fetch('/api/farmacia/lote', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify(body)
                    })
                    .then(r => { if (!r.ok) { return r.json().then(e => { throw new Error(e.error || 'Error ' + r.status); }); } return r.json(); })
                    .then(res => {
                        msgDiv.innerHTML = `<div class="alert alert-success py-2 small fw-800">âœ” ${res.mensaje} â€” ${res.medicamento} (${res.numeroLote}) â€” ${res.cantidadInicial} und.</div>`;
                        btn.disabled = false;
                        btn.innerHTML = '<i class="bi bi-check-lg me-1"></i> GUARDAR LOTE';
                        cargarStock();
                        cargarAlertas();
                        document.getElementById('formNuevoLote').reset();
                        document.getElementById('loteMedId').value = '';
                        setTimeout(() => {
                            const modal = bootstrap.Modal.getInstance(document.getElementById('modalNuevoLote'));
                            if (modal) modal.hide();
                        }, 1500);
                    })
                    .catch(e => {
                        msgDiv.innerHTML = `<div class="alert alert-danger py-2 small fw-800">${e.message}</div>`;
                        btn.disabled = false;
                        btn.innerHTML = '<i class="bi bi-check-lg me-1"></i> GUARDAR LOTE';
                    });
                });
            }
        });
    
