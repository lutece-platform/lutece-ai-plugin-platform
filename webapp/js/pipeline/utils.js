/**
 * Escapes special HTML characters in a string to prevent XSS and broken attributes.
 * @param {*} str the value to escape
 * @return {string} the escaped string
 */
function esc(str) {
  if (str == null) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

export function fieldHTML(name, def, val = '', context = {}) {
  const fieldValue = val || def.defaultValue || '';
  const fieldId = `field_${name.replace(/\W/g, '_')}`;
  const labelText = def.title || name;
  const description = def.description ? `<div class="form-text text-muted small mb-2">${def.description}</div>` : '';
  const req = def.required ? 'required' : '';
  
  const standardWrapper = (content) => `
    <div class="mb-3">
      <label for="${fieldId}" class="form-label fw-medium mb-0">
        ${labelText}
        ${req ? '<span class="text-danger">*</span>' : ''}
      </label>
      ${description}
      ${content}
    </div>`;

  switch (def.type) {
    case 'NUMBER':
      return standardWrapper(`
        <div class="input-group has-validation">
          ${def.prepend ? `<span class="input-group-text">${def.prepend}</span>` : ''}
          <input type="number" class="form-control ${def.required ? 'required' : ''}" 
                 id="${fieldId}" name="${name}" value="${fieldValue}"
                 ${def.min !== undefined ? `min="${def.min}"` : ''}
                 ${def.max !== undefined ? `max="${def.max}"` : ''}
                 ${def.step !== undefined ? `step="${def.step}"` : ''}
                 ${req} placeholder="${def.placeholder || ''}">
          ${def.append ? `<span class="input-group-text">${def.append}</span>` : ''}
          ${def.required ? `<div class="invalid-feedback">Ce champ est requis</div>` : ''}
        </div>`);

    case 'BOOLEAN':
      // For boolean, check if fieldValue is truthy
      const isChecked = fieldValue === true || fieldValue === 'true' || fieldValue === '1';
      return `
        <div class="form-check form-switch mb-3">
          <div class="d-flex align-items-start">
            <div class="me-3">
              <input class="form-check-input" type="checkbox" role="switch" 
                     id="${fieldId}" name="${name}" ${isChecked ? 'checked' : ''}>
            </div>
            <div>
              <label class="form-check-label" for="${fieldId}">${labelText}</label>
              ${description}
            </div>
          </div>
        </div>`;

    case 'ENUM':
      if (Array.isArray(def.options)) {
        const isVariable = typeof fieldValue === 'string' && fieldValue.startsWith('{{') && fieldValue.endsWith('}}');
        const selectId = `${fieldId}_select`;
        const inputId = `${fieldId}_input`;
        const checkId = `${fieldId}_check`;
        
        const opts = def.options
          .map(o => {
            const optionValue = o.value !== undefined ? o.value : o;
            const optionLabel = o.label !== undefined ? o.label : optionValue;
            const selected = !isVariable && optionValue === fieldValue ? 'selected' : '';
            return `<option value="${optionValue}" ${selected}>${optionLabel}</option>`;
          })
          .join('');

        const toggleScript = `
          const select = document.getElementById('${selectId}');
          const input = document.getElementById('${inputId}');
          const isChecked = this.checked;
          select.disabled = isChecked;
          input.disabled = !isChecked;
          if (isChecked) {
            input.focus();
          } else {
            select.focus();
          }`;

        return standardWrapper(`
          <div class="d-flex flex-column">
            <div class="input-group mb-2">
              <select class="form-select ${def.required ? 'required' : ''}" 
                      id="${selectId}" name="${name}_select" ${req} ${isVariable ? 'disabled' : ''}>
                <option value="" ${!fieldValue && !isVariable ? 'selected' : ''} disabled>Sélectionnez une option...</option>
                ${opts}
              </select>
              <input type="text" class="form-control tribute-enabled" 
                     id="${inputId}" name="${name}_input" 
                     value="${isVariable ? fieldValue : ''}" 
                     placeholder="Variable : {{variable}}" ${!isVariable ? 'disabled' : ''}>
            </div>
            <div class="form-check form-switch mt-1">
              <input class="form-check-input" type="checkbox" id="${checkId}" 
                     name="${name}_use_variable" 
                     onchange="${toggleScript.replace(/"/g, '&quot;')}" ${isVariable ? 'checked' : ''}>
              <label class="form-check-label small text-muted" for="${checkId}">
                Utiliser une variable
              </label>
            </div>
          </div>`);
      }
      return standardWrapper(`<div class="alert alert-warning">Configuration ENUM invalide pour ${name}</div>`);

    case 'ARRAY': {
      const itemSchema = def.itemSchema || {};

      if (itemSchema.type === 'ENUM' && Array.isArray(itemSchema.options)) {
        const arrayValue = Array.isArray(fieldValue) ? fieldValue : [];
        const checkboxesHtml = itemSchema.options
          .filter(o => o.value !== '')
          .map((o, index) => {
            const optionValue = o.value !== undefined ? o.value : o;
            const optionLabel = o.label !== undefined ? o.label : optionValue;
            const optionDesc = o.description || '';
            const isChecked = arrayValue.includes(optionValue) || arrayValue.includes(String(optionValue));
            const checkId = `${fieldId}_${index}`;
            return `
              <div class="form-check mb-2">
                <input class="form-check-input" type="checkbox"
                       id="${checkId}" name="${name}[]" value="${optionValue}"
                       ${isChecked ? 'checked' : ''}>
                <label class="form-check-label" for="${checkId}">
                  ${optionLabel}
                  ${optionDesc ? `<small class="text-muted d-block">${optionDesc}</small>` : ''}
                </label>
              </div>`;
          }).join('');

        return standardWrapper(`
          <div class="enum-array-field rounded p-3 bg-light" data-field="${name}" data-schema-type="enum-array">
            ${checkboxesHtml || '<div class="text-muted small">Aucune option disponible</div>'}
          </div>`);
      }

      const schemaFields = (itemSchema.type === 'OBJECT' && itemSchema.fields) ? itemSchema.fields : [];

      if (schemaFields.length === 0) {
        return standardWrapper(`<div class="alert alert-warning">Configuration ARRAY invalide pour ${name} : Le schéma ne définit aucun champ.</div>`);
      }

      const arrayValue = Array.isArray(fieldValue) ? fieldValue : [];

      let entriesHTML = '';
      if (arrayValue.length > 0) {
        entriesHTML = arrayValue.map((item, index) => {
          const fieldsHtml = schemaFields.map(field => {
            const itemFieldValue = item[field.name] || field.defaultValue || '';
            return `
              <div class="mb-2">
                <label class="form-label small">${field.description || field.name}</label>
                <input type="text" class="form-control tribute-enabled"
                       name="${name}__${field.name}[]" value="${itemFieldValue}"
                       placeholder="${field.description || ''}">
              </div>`;
          }).join('');

          return `
            <div class="card mb-3 object-entry shadow-sm">
              <div class="card-body p-3">
                ${fieldsHtml}
                <button type="button" class="btn btn-sm btn-outline-danger remove-object-item mt-3">
                  <i class="ti ti-trash me-1"></i>Supprimer
                </button>
              </div>
            </div>`;
        }).join('');
      }

      const templateFieldsHtml = schemaFields.map(field => {
        return `
          <div class="mb-2">
            <label class="form-label small">${field.description || field.name}</label>
            <input type="text" class="form-control tribute-enabled"
                   name="${name}__${field.name}[]"
                   placeholder="${field.description || field.defaultValue || ''}">
          </div>`;
      }).join('');

      const templateHtml = `
        <div class="card mb-3 object-entry shadow-sm">
          <div class="card-body p-3">
            ${templateFieldsHtml}
            <button type="button" class="btn btn-sm btn-outline-danger remove-object-item mt-3">
              <i class="ti ti-trash me-1"></i>Supprimer
            </button>
          </div>
        </div>`;

      return standardWrapper(`
        <div class="object-field rounded p-3 bg-light" data-field="${name}" data-schema-type="object">
          <div class="object-entries mb-3">
            ${entriesHTML || `
              <div class="text-muted small mb-3 fst-italic">
                <i class="ti ti-info-circle me-1"></i>
                Aucun élément. Cliquez sur "Ajouter" pour créer des items.
              </div>`}
          </div>
          <button type="button" class="btn btn-sm btn-primary add-object-field">
            <i class="ti ti-plus me-1"></i>Ajouter un élément
          </button>
          <template class="object-entry-template">
            ${templateHtml}
          </template>
        </div>`);
    }

    case 'FILE': {
      const fileVars = (context.inputSchema || []).filter(f => f.type === 'FILE');
      const opts = fileVars.map(f => {
        const ref = `{{sys.${f.name}}}`;
        const label = f.title || f.name;
        const badge = f.multiple ? ' (multiple)' : '';
        const selected = fieldValue === ref ? 'selected' : '';
        return `<option value="${ref}" ${selected}>${label}${badge}</option>`;
      }).join('');
      return standardWrapper(`
        <select class="form-select ${def.required ? 'required' : ''}"
                id="${fieldId}" name="${name}" ${req}>
          <option value="" ${!fieldValue ? 'selected' : ''} disabled>Sélectionnez un champ fichier...</option>
          ${opts || '<option value="" disabled>Aucun champ fichier défini dans le nœud Départ</option>'}
        </select>`);
    }

    case 'STRING':
      if (def.isTextarea) {
        const rows = def.rows || 3;
        return standardWrapper(`
          <textarea class="form-control tribute-enabled ${def.required ? 'required' : ''}"
                    id="${fieldId}" name="${name}" rows="${rows}"
                    placeholder="${esc(def.placeholder)}" ${req}>${esc(fieldValue)}</textarea>
          ${def.required ? `<div class="invalid-feedback">Ce champ est requis</div>` : ''}`);
      } else {
        return standardWrapper(`
          <input type="text" class="form-control tribute-enabled ${def.required ? 'required' : ''}"
                 id="${fieldId}" name="${name}" value="${esc(fieldValue)}"
                 placeholder="${esc(def.placeholder)}" ${req}>
          ${def.required ? `<div class="invalid-feedback">Ce champ est requis</div>` : ''}`);
      }

    default:
      return standardWrapper(`
        <input type="text" class="form-control tribute-enabled ${def.required ? 'required' : ''}"
               id="${fieldId}" name="${name}" value="${esc(fieldValue)}"
               placeholder="${esc(def.placeholder)}" ${req}>
        ${def.required ? `<div class="invalid-feedback">Ce champ est requis</div>` : ''}`);
  }
}