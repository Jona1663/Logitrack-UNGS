let pristine;

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");

    pristine = new Pristine(form, {
        classTo: 'input-group', // Se adapta a la nueva estructura HTML
        errorClass: 'has-danger',
        successClass: 'has-success',
        errorTextParent: 'input-group', // Muestra el error debajo del input group
        errorTextTag: 'div',
        errorTextClass: 'text-danger small mt-1 w-100'
    });

    // ─── Referencias a los campos ────────────────────────────────
    const inputBuscarCliente = document.getElementById('inputBuscarCliente');
    const clienteCuitSeleccionado = document.getElementById('clienteCuitSeleccionado');
    const selectOrigen = document.getElementById('selectOrigen');
    const selectDestino = document.getElementById('selectDestino');
    const peso = document.getElementById('peso');
    const inputBuscarGrano = document.getElementById('inputBuscarGrano');
    const granoIdSeleccionado = document.getElementById('granoIdSeleccionado');
    const notas = document.getElementById('notas');

    // ─── Validadores ─────────────────────────────────────────────

    // Valida que se haya seleccionado un cliente del autocompletado (el hidden input debe tener valor)
    pristine.addValidator(inputBuscarCliente,
        () => clienteCuitSeleccionado.value.trim() !== '',
        'Debe seleccionar una empresa cliente de la lista.'
    );

    // Valida que origen y destino estén seleccionados
    pristine.addValidator(selectOrigen,
        value => value.trim() !== '',
        'Debe seleccionar un establecimiento de origen.'
    );

    pristine.addValidator(selectDestino,
        value => value.trim() !== '',
        'Debe seleccionar un establecimiento de destino.'
    );

    // Valida peso > 0
    pristine.addValidator(peso,
        value => value.trim() !== '' && parseFloat(value) > 0,
        'El peso debe ser mayor a 0 Tn.'
    );

    // Valida que se haya seleccionado un grano (el hidden input debe tener valor)
    pristine.addValidator(inputBuscarGrano,
        () => granoIdSeleccionado.value.trim() !== '',
        'Debe seleccionar un tipo de grano de la lista.'
    );

    // Notas (opcional pero con límite)
    pristine.addValidator(notas,
        value => value.trim().length <= 200,
        'Las observaciones no pueden superar los 200 caracteres.'
    );

    // Valida que destino esté seleccionado Y sea diferente al origen
    pristine.addValidator(selectDestino,
        function (value) {
            return value.trim() !== '' && value !== selectOrigen.value;
        },
        'El punto de descarga debe ser diferente al origen.'
    );

    // ─── Validación al salir del campo (Blur) ─────────────────────
    form.querySelectorAll('input, select, textarea').forEach(input => {
        input.addEventListener('blur', () => pristine.validate(input));
    });
});