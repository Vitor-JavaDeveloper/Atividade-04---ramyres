/*
  front-end do healthcare (javascript puro, sem framework).

  como funciona, resumindo:
  1. o usuario faz login; a api devolve um token e guardamos no navegador (localstorage).
  2. toda chamada para a api manda esse token no cabecalho authorization.
  3. o menu e montado conforme o perfil (recepcionista, enfermeiro, medico, administrador).
  4. cada tela e uma <section> do index.html; mostramos uma de cada vez usando o "#" do endereco
     (ex: localhost:8088/#fila). assim o botao voltar do navegador e o f5 funcionam.
*/

// ======================= constantes =======================

// nomes bonitos das gravidades (rd01) e dos status da fila
const GRAVIDADES = {
  EMERGENCIA: 'Emergência', MUITO_URGENTE: 'Muito Urgente', URGENTE: 'Urgente',
  POUCO_URGENTE: 'Pouco Urgente', NAO_URGENTE: 'Não Urgente'
};
const STATUS = { AGUARDANDO: 'Aguardando', EM_ATENDIMENTO: 'Em atendimento', FINALIZADO: 'Finalizado', CANCELADO: 'Cancelado' };
const PERFIS = { ADMINISTRADOR: 'Administrador', RECEPCIONISTA: 'Recepcionista', ENFERMEIRO: 'Enfermeiro(a)', MEDICO: 'Médico(a)' };

// telas do menu e quais perfis enxergam cada uma (rf06: acesso por perfil)
const MENU = [
  { id: 'fila', nome: 'Fila de Atendimento', perfis: ['ADMINISTRADOR', 'RECEPCIONISTA', 'ENFERMEIRO', 'MEDICO'] },
  { id: 'cadastro-paciente', nome: 'Cadastro de Paciente', perfis: ['ADMINISTRADOR', 'RECEPCIONISTA'] },
  { id: 'cadastro-medico', nome: 'Médicos', perfis: ['ADMINISTRADOR', 'RECEPCIONISTA', 'MEDICO'] },
  { id: 'triagem', nome: 'Triagem', perfis: ['ADMINISTRADOR', 'ENFERMEIRO'] },
  { id: 'historico', nome: 'Histórico do Paciente', perfis: ['ADMINISTRADOR', 'MEDICO'] },
  { id: 'usuarios', nome: 'Usuários', perfis: ['ADMINISTRADOR'] },
  { id: 'relatorios', nome: 'Relatórios', perfis: ['ADMINISTRADOR'] }
];

// usuario logado (preenchido depois do login)
let usuario = null;
// id do atendimento aberto na tela de atendimento
let atendimentoAtual = null;
// timer que atualiza a fila sozinha
let timerFila = null;

// atalho para pegar elementos pelo id
const $ = (id) => document.getElementById(id);

// ======================= comunicacao com a api =======================

/**
 * faz uma chamada para a api e devolve o json.
 * se der erro, lanca um objeto com a mensagem e os campos errados.
 * se a sessao expirou (401), volta para a tela de login.
 */
async function api(caminho, opcoes = {}) {
  const resposta = await fetch('/api' + caminho, {
    ...opcoes,
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + (localStorage.getItem('token') || '')
    },
    body: opcoes.body ? JSON.stringify(opcoes.body) : undefined
  });
  const dados = await resposta.json().catch(() => ({}));
  if (!resposta.ok) {
    if (resposta.status === 401 && caminho !== '/auth/login') sair();
    throw { mensagem: dados.erro || 'Falha ao falar com o servidor.', campos: dados.campos || {} };
  }
  return dados;
}

// ======================= ajudantes de tela =======================

/** escapa texto antes de colocar no html (evita que um nome com <script> vire codigo). */
function esc(texto) {
  return String(texto ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** mostra uma mensagem verde (sucesso) ou vermelha (erro) no topo da tela. */
function avisar(msg, erro = false) {
  const el = $('aviso');
  el.textContent = msg;
  el.className = 'aviso' + (erro ? ' erro' : '');
  el.hidden = false;
  clearTimeout(avisar.timer);
  avisar.timer = setTimeout(() => (el.hidden = true), 5000);
}

/** pega os campos de um formulario e transforma em objeto { nome: valor }. */
function lerFormulario(form) {
  return Object.fromEntries(new FormData(form).entries());
}

/** limpa as marcacoes de erro de um formulario. */
function limparErros(form) {
  form.querySelectorAll('.invalido').forEach((el) => el.classList.remove('invalido'));
  form.querySelectorAll('small.erro').forEach((el) => (el.textContent = ''));
}

/** marca em vermelho cada campo que a api disse estar errado (criterio do hu01). */
function mostrarErros(form, erro) {
  limparErros(form);
  Object.entries(erro.campos || {}).forEach(([campo, msg]) => {
    const entrada = form.querySelector(`[name="${campo}"]`);
    const aviso = form.querySelector(`small[data-campo="${campo}"]`) || entrada?.closest('label')?.querySelector('small.erro');
    entrada?.classList.add('invalido');
    if (aviso) aviso.textContent = msg;
  });
  avisar(erro.mensagem, true);
}

/** quando o usuario mexe num campo marcado em vermelho, tira a marcacao daquele campo. */
document.addEventListener('change', limparCampo);
document.addEventListener('input', limparCampo);
function limparCampo(e) {
  if (!e.target.classList) return;
  const form = e.target.form;
  form?.querySelectorAll(`[name="${e.target.name}"]`).forEach((el) => el.classList.remove('invalido'));
  const aviso = form?.querySelector(`small[data-campo="${e.target.name}"]`) || e.target.closest('label')?.querySelector('small.erro');
  if (aviso) aviso.textContent = '';
}

// formatacoes simples de data, cpf e tempo
const dataBR = (iso) => (iso ? iso.substring(0, 10).split('-').reverse().join('/') : '');
const dataHoraBR = (iso) => (iso ? dataBR(iso) + ' ' + iso.substring(11, 16) : '');
const cpfBR = (c) => (c || '').replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
function minutosBR(min) {
  if (min < 60) return min + ' min';
  return Math.floor(min / 60) + 'h' + (min % 60 ? ' ' + (min % 60) + 'min' : '');
}
const badge = (g) => `<span class="badge g-${g}">${GRAVIDADES[g]}</span>`;

/** mascaras de digitacao para cpf e telefone. */
document.addEventListener('input', (e) => {
  const tipo = e.target.dataset.mascara;
  if (!tipo) return;
  let v = e.target.value.replace(/\D/g, '');
  if (tipo === 'cpf') {
    v = v.slice(0, 11).replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d{1,2})$/, '$1-$2');
  } else {
    v = v.slice(0, 11).replace(/^(\d{2})(\d)/, '($1) $2').replace(/(\d{4,5})(\d{4})$/, '$1-$2');
  }
  e.target.value = v;
});

// ======================= login e navegacao =======================

/** envio do formulario de login (rf06). */
$('formLogin').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('erroLogin').textContent = '';
  try {
    const r = await api('/auth/login', { method: 'POST', body: lerFormulario(e.target) });
    localStorage.setItem('token', r.token);
    entrar(r.usuario, r.telaInicial);
  } catch (erro) {
    $('erroLogin').textContent = erro.mensagem; // a api devolve a mensagem de usuario ou senha incorretos
  }
});

/** depois do login: monta o menu do perfil e vai para a tela inicial dele. */
function entrar(u, telaInicial) {
  usuario = u;
  $('telaLogin').hidden = true;
  $('sistema').hidden = false;
  $('nomeUsuario').textContent = u.nome;
  $('perfilUsuario').textContent = PERFIS[u.perfil];
  $('menu').innerHTML = MENU.filter((m) => m.perfis.includes(u.perfil))
    .map((m) => `<a href="#${m.id}" data-tela="${m.id}">${m.nome}</a>`).join('');
  // esconde botoes/formularios que nao sao do perfil (atributo data-perfis no html)
  document.querySelectorAll('[data-perfis]').forEach((el) => (el.hidden = !el.dataset.perfis.includes(u.perfil)));
  const atual = location.hash.slice(1);
  // se ja estava numa tela permitida (f5), fica nela; senao vai para a tela inicial do perfil
  if ((atual && podeVer(atual.split('/')[0])) || atual === telaInicial) mostrarTela(); else location.hash = telaInicial;
}

/** sai do sistema: avisa a api, apaga o token e volta para o login. */
function sair() {
  if (localStorage.getItem('token')) fetch('/api/auth/logout', { method: 'POST', headers: { Authorization: 'Bearer ' + localStorage.getItem('token') } });
  localStorage.removeItem('token');
  usuario = null;
  clearInterval(timerFila);
  $('sistema').hidden = true;
  $('telaLogin').hidden = false;
  $('formLogin').reset();
}
$('btnSair').addEventListener('click', sair);

const podeVer = (tela) => tela === 'atendimento' ? usuario.perfil === 'MEDICO' : MENU.some((m) => m.id === tela && m.perfis.includes(usuario.perfil));

/** mostra a tela que esta no "#" do endereco e carrega os dados dela. */
function mostrarTela() {
  if (!usuario) return;
  const [tela, parametro] = location.hash.slice(1).split('/');
  if (!podeVer(tela)) return;
  document.querySelectorAll('.tela').forEach((s) => (s.hidden = s.id !== 'tela-' + tela));
  document.querySelectorAll('#menu a').forEach((a) => a.classList.toggle('ativo', a.dataset.tela === tela));
  clearInterval(timerFila);
  const carregar = { fila: carregarFila, 'cadastro-paciente': carregarPacientes, 'cadastro-medico': carregarMedicos,
    triagem: carregarTriagem, historico: () => carregarHistorico(parametro), atendimento: () => carregarAtendimento(parametro),
    usuarios: carregarUsuarios, relatorios: carregarRelatorio }[tela];
  carregar().catch((erro) => avisar(erro.mensagem, true));
  if (tela === 'fila') timerFila = setInterval(() => carregarFila().catch(() => {}), 10000); // atualiza a cada 10s
}
window.addEventListener('hashchange', mostrarTela);

// ======================= fila de atendimento (rf04) =======================

async function carregarFila() {
  const dados = await api('/fila');
  const souMedico = usuario.perfil === 'MEDICO';

  // quadro com o tempo estimado para quem chega agora em cada gravidade (hu08)
  $('infoMedicos').textContent = `(${dados.medicosDisponiveis} médico(s) disponível(is), consulta média de ${dados.tempoMedioConsultaMin} min)`;
  $('temposGravidade').innerHTML = dados.temposPorGravidade.map((t) => `
    <div class="tempo g-${t.gravidade}">${t.descricao}
      <b>${minutosBR(t.tempoEstimadoMin)}</b>
      <small>${t.aguardando} aguardando · máx. ${minutosBR(t.tempoMaximoReferenciaMin)}</small></div>`).join('');

  // tabela da fila, ja vem ordenada da api (gravidade e depois chegada)
  $('tabelaFila').innerHTML = dados.itens.map((f) => {
    const p = f.ficha.paciente;
    const meu = f.medico && f.medico.id === usuario.id;
    let acoes = '';
    if (['MEDICO', 'ADMINISTRADOR'].includes(usuario.perfil)) acoes += `<a class="btn pequeno" href="#historico/${p.id}" title="Ver histórico">&#128065; Histórico</a>`;
    if (souMedico && f.status === 'AGUARDANDO') acoes += `<button class="btn pequeno" onclick="chamar(${f.id})">Chamar</button>`;
    if (souMedico && meu) acoes += `<a class="btn pequeno primario" href="#atendimento/${f.id}">Atender</a>`;
    if (!souMedico && f.status === 'AGUARDANDO') acoes += `<button class="btn pequeno perigo" onclick="cancelar(${f.id})">Retirar</button>`;
    return `<tr class="${f.status === 'EM_ATENDIMENTO' ? 'em-atendimento' : ''}">
      <td>${f.posicao || ''}</td>
      <td><b>${esc(p.nome)}</b></td>
      <td>${badge(f.ficha.gravidade)}</td>
      <td>${f.status === 'AGUARDANDO' ? '&#128339; ' + minutosBR(f.tempoEsperaEstimado) : ''}</td>
      <td class="${f.acimaDoTempoMaximo ? 'atrasado' : ''}" title="${f.acimaDoTempoMaximo ? 'Passou do tempo máximo da gravidade' : ''}">${minutosBR(f.minutosEsperando)}</td>
      <td>${STATUS[f.status]}${f.medico ? '<br><small>' + esc(f.medico.nome) + '</small>' : ''}</td>
      <td class="direita">${acoes}</td></tr>`;
  }).join('') || '<tr><td colspan="7" class="vazio">Nenhum paciente na fila.</td></tr>';
}

/** medico chama um paciente especifico e vai direto para a tela de atendimento. */
async function chamar(id) {
  try {
    const f = await api(`/fila/${id}/chamar`, { method: 'POST' });
    location.hash = 'atendimento/' + f.id;
  } catch (erro) { avisar(erro.mensagem, true); }
}

/** botao "chamar proximo": a api escolhe o mais prioritario. */
$('btnChamarProximo').addEventListener('click', async () => {
  try {
    const f = await api('/fila/chamar-proximo', { method: 'POST' });
    avisar('Chamando ' + f.ficha.paciente.nome);
    location.hash = 'atendimento/' + f.id;
  } catch (erro) { avisar(erro.mensagem, true); }
});

/** retira o paciente da fila (desistencia, transferencia). */
async function cancelar(id) {
  if (!confirm('Retirar este paciente da fila?')) return;
  try {
    await api(`/fila/${id}/cancelar`, { method: 'POST' });
    avisar('Paciente retirado da fila.');
    carregarFila();
  } catch (erro) { avisar(erro.mensagem, true); }
}

// ======================= pacientes (rf01) =======================

async function carregarPacientes() {
  const busca = $('buscaPaciente').value;
  const lista = await api('/pacientes' + (busca ? '?busca=' + encodeURIComponent(busca) : ''));
  $('tabelaPacientes').innerHTML = lista.map((p) => `<tr>
      <td><b>${esc(p.nome)}</b></td><td>${cpfBR(p.cpf)}</td><td>${dataBR(p.dataNascimento)}</td>
      <td>${p.idade} anos</td><td>${esc(p.telefone)}</td>
      <td class="direita">${usuario.perfil === 'ADMINISTRADOR' ? `<a class="btn pequeno" href="#historico/${p.id}">Histórico</a>` : ''}</td></tr>`).join('')
    || '<tr><td colspan="6" class="vazio">Nenhum paciente encontrado.</td></tr>';
}
$('buscaPaciente').addEventListener('input', () => carregarPacientes().catch(() => {}));

$('formPaciente').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const p = await api('/pacientes', { method: 'POST', body: lerFormulario(e.target) });
    e.target.reset();
    limparErros(e.target);
    avisar(`Paciente ${p.nome} cadastrado com sucesso.`);
    $('buscaPaciente').value = '';
    carregarPacientes(); // criterio do hu01: aparece na lista de cadastrados
  } catch (erro) { mostrarErros(e.target, erro); }
});
$('formPaciente').addEventListener('reset', (e) => limparErros(e.target));

// ======================= medicos (rf02 e rf07) =======================

async function carregarMedicos() {
  const lista = await api('/medicos?disponiveis=' + $('soDisponiveis').checked);
  const gerencia = ['RECEPCIONISTA', 'ADMINISTRADOR'].includes(usuario.perfil);
  $('tabelaMedicos').innerHTML = lista.map((m) => {
    const podeMudar = gerencia || m.id === usuario.id; // medico so muda a propria disponibilidade
    return `<tr><td><b>${esc(m.nome)}</b></td><td>${esc(m.crm)}</td><td>${esc(m.especialidade)}</td>
      <td><span class="badge ${m.disponivel ? 'disp-sim' : 'disp-nao'}">${m.disponivel ? 'Disponível' : 'Indisponível'}</span></td>
      <td class="direita">${podeMudar ? `<button class="btn pequeno" onclick="alternarDisponibilidade(${m.id}, ${!m.disponivel})">Marcar ${m.disponivel ? 'indisponível' : 'disponível'}</button>` : ''}</td></tr>`;
  }).join('') || '<tr><td colspan="5" class="vazio">Nenhum médico encontrado.</td></tr>';
}
$('soDisponiveis').addEventListener('change', () => carregarMedicos().catch(() => {}));

async function alternarDisponibilidade(id, disponivel) {
  try {
    await api(`/medicos/${id}/disponibilidade`, { method: 'PATCH', body: { disponivel } });
    carregarMedicos();
  } catch (erro) { avisar(erro.mensagem, true); }
}

$('formMedico').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const m = await api('/medicos', { method: 'POST', body: lerFormulario(e.target) });
    e.target.reset();
    limparErros(e.target);
    avisar(`Médico ${m.nome} cadastrado com sucesso.`);
    carregarMedicos();
  } catch (erro) { mostrarErros(e.target, erro); }
});
$('formMedico').addEventListener('reset', (e) => limparErros(e.target));

// ======================= triagem (rf03) =======================

/** preenche as listas de pacientes e de medicos disponiveis. */
async function carregarTriagem() {
  const [pacientes, medicos] = await Promise.all([api('/pacientes'), api('/medicos?disponiveis=true')]);
  $('selectPaciente').innerHTML = '<option value="">Selecione o paciente...</option>' +
    pacientes.map((p) => `<option value="${p.id}">${esc(p.nome)} (CPF ${cpfBR(p.cpf)})</option>`).join('');
  $('selectMedico').innerHTML = '<option value="">Definir quando o médico chamar</option>' +
    medicos.map((m) => `<option value="${m.id}">${esc(m.nome)} (${esc(m.especialidade)})</option>`).join('');
}

$('formTriagem').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const f = await api('/triagens', { method: 'POST', body: lerFormulario(e.target) });
    e.target.reset();
    limparErros(e.target);
    // criterio do hu02: ao salvar, o paciente entra na fila automaticamente
    avisar(`Ficha salva. ${f.ficha.paciente.nome} entrou na fila na posição ${f.posicao} (${GRAVIDADES[f.ficha.gravidade]}).`);
  } catch (erro) { mostrarErros(e.target, erro); }
});

// ======================= historico do paciente (rf05) =======================

async function carregarHistorico(pacienteId) {
  const pacientes = await api('/pacientes');
  $('selectHistorico').innerHTML = '<option value="">Escolha um paciente...</option>' +
    pacientes.map((p) => `<option value="${p.id}" ${String(p.id) === pacienteId ? 'selected' : ''}>${esc(p.nome)}</option>`).join('');
  if (!pacienteId) { $('historicoConteudo').innerHTML = ''; return; }

  const h = await api(`/pacientes/${pacienteId}/historico`);
  const p = h.paciente;
  $('historicoConteudo').innerHTML = `
    <div class="card paciente-topo">
      <div><h3>${esc(p.nome)}</h3>
        <div class="info">&#128197; ${dataBR(p.dataNascimento)} (${p.idade} anos) &nbsp; &#128222; ${esc(p.telefone)} &nbsp; &#128205; ${esc(p.endereco)}</div></div>
      <div class="direita"><small>CPF</small><br><b>${cpfBR(p.cpf)}</b></div>
    </div>
    <h3>Consultas Anteriores</h3>
    ${h.consultas.map((c) => `
      <div class="card consulta">
        <span class="data">${dataBR(c.dataConsulta)}</span> ${c.ficha ? badge(c.ficha.gravidade) : ''}
        <h3 style="margin-top:10px">${esc(c.diagnostico)}</h3>
        <p><b>Observações:</b><br>${esc(c.observacoes) || '-'}</p>
        <p><b>Prescrição:</b></p><div class="caixa">${esc(c.prescricao) || '-'}</div>
        <p><small>Atendido por ${esc(c.medico?.nome)}${c.ficha ? ' · Sintomas na triagem: ' + esc(c.ficha.sintomas) : ''}</small></p>
      </div>`).join('') || '<div class="card vazio">Nenhuma consulta anterior.</div>'}
    <h3>Triagens</h3>
    <div class="card sem-padding"><table><thead><tr><th>Data</th><th>Gravidade</th><th>Sintomas</th><th>Enfermeiro</th><th>Médico responsável</th></tr></thead><tbody>
    ${h.triagens.map((t) => `<tr><td>${dataHoraBR(t.dataHora)}</td><td>${badge(t.gravidade)}</td><td>${esc(t.sintomas)}</td>
      <td>${esc(t.enfermeiro?.nome)}</td><td>${esc(t.medico?.nome)}</td></tr>`).join('') || '<tr><td colspan="5" class="vazio">Nenhuma triagem.</td></tr>'}
    </tbody></table></div>`;
}
$('selectHistorico').addEventListener('change', (e) => (location.hash = 'historico/' + e.target.value));

// ======================= atendimento =======================

/** mostra a ficha de triagem do paciente chamado e o formulario da consulta. */
async function carregarAtendimento(id) {
  const f = await api('/fila/' + id);
  atendimentoAtual = f.id;
  const p = f.ficha.paciente;
  const ativo = f.status === 'EM_ATENDIMENTO' && f.medico?.id === usuario.id;
  $('formAtendimento').hidden = !ativo;
  $('atendimentoFicha').innerHTML = `
    <div class="paciente-topo"><div><h3>${esc(p.nome)}</h3>
      <div class="info">${p.idade} anos · ${esc(p.sexo)} · CPF ${cpfBR(p.cpf)}</div></div>
      <div>${badge(f.ficha.gravidade)}</div></div>
    <p><b>Sintomas registrados na triagem (${dataHoraBR(f.ficha.dataHora)}):</b></p>
    <div class="caixa">${esc(f.ficha.sintomas)}</div>
    <p>Esperou ${minutosBR(f.minutosEsperando)} · Situação: <b>${STATUS[f.status]}</b> ·
      <a href="#historico/${p.id}">ver histórico médico do paciente</a></p>`;
}

$('formAtendimento').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await api(`/fila/${atendimentoAtual}/finalizar`, { method: 'POST', body: lerFormulario(e.target) });
    e.target.reset();
    limparErros(e.target);
    avisar('Atendimento finalizado e registrado no histórico.');
    location.hash = 'fila';
  } catch (erro) { mostrarErros(e.target, erro); }
});

$('btnCancelarAtendimento').addEventListener('click', async () => {
  if (!confirm('Cancelar este atendimento? O paciente sai da fila sem consulta.')) return;
  try {
    await api(`/fila/${atendimentoAtual}/cancelar`, { method: 'POST' });
    avisar('Atendimento cancelado.');
    location.hash = 'fila';
  } catch (erro) { avisar(erro.mensagem, true); }
});

// ======================= usuarios / funcionarios (so o administrador) =======================

// guarda o usuario que esta sendo editado (null = cadastro novo)
let usuarioEmEdicao = null;

/** mostra ou esconde os campos que dependem do perfil escolhido. */
function atualizarCamposPerfil() {
  const perfil = $('formUsuario').perfil.value;
  const editando = !!usuarioEmEdicao;
  $('camposAdmin').hidden = perfil !== 'ADMINISTRADOR';
  $('camposRecep').hidden = perfil !== 'RECEPCIONISTA';
  $('camposEnf').hidden = perfil !== 'ENFERMEIRO';
  $('camposMedico').hidden = perfil !== 'MEDICO';
  // na edicao nao da para trocar o medico vinculado, so alterar crm e especialidade
  $('vincularMedico').hidden = perfil !== 'MEDICO' || editando;
  atualizarDadosNovoMedico();
}

/** se o admin escolheu um medico ja cadastrado, esconde crm e especialidade. */
function atualizarDadosNovoMedico() {
  const ligar = !$('vincularMedico').hidden && $('selectMedicoSemAcesso').value;
  $('dadosNovoMedico').hidden = $('camposMedico').hidden || !!ligar;
}

$('formUsuario').perfil.addEventListener('change', atualizarCamposPerfil);
$('selectMedicoSemAcesso').addEventListener('change', atualizarDadosNovoMedico);

/** coloca o formulario em modo novo ou em modo edicao. */
function modoFormUsuario(u) {
  usuarioEmEdicao = u || null;
  const form = $('formUsuario');
  form.reset();
  limparErros(form);
  $('tituloFormUsuario').textContent = u ? 'Editar usuário' : 'Novo usuário';
  $('btnSalvarUsuario').textContent = u ? 'Salvar alterações' : 'Cadastrar usuário';
  $('campoSenha').hidden = !!u; // na edicao a senha muda pelo botao redefinir
  form.perfil.disabled = !!u;   // o perfil nao muda depois do cadastro
  if (u) {
    form.nome.value = u.nome || '';
    form.login.value = u.login || '';
    form.perfil.value = u.perfil || '';
    if (u.nivelAcesso) form.nivelAcesso.value = u.nivelAcesso;
    if (u.matricula) form.matricula.value = u.matricula;
    if (u.coren) form.coren.value = u.coren;
    if (u.crm) form.crm.value = u.crm;
    if (u.especialidade) form.especialidade.value = u.especialidade;
    if (u.disponivel != null) form.disponivel.value = String(!!u.disponivel);
  }
  atualizarCamposPerfil();
}

async function carregarUsuarios() {
  const [lista, medicos] = await Promise.all([api('/usuarios'), api('/medicos')]);
  const semAcesso = medicos.filter((m) => !m.temAcesso);
  $('selectMedicoSemAcesso').innerHTML = '<option value="">Cadastrar novo médico</option>' +
    semAcesso.map((m) => `<option value="${m.id}">${esc(m.nome)} (${esc(m.crm)})</option>`).join('');
  if (!usuarioEmEdicao) modoFormUsuario(null);

  $('tabelaUsuarios').innerHTML = lista.map((u) => {
    const eu = u.id === usuario.id;
    const situacao = u.ativo
      ? '<span class="badge disp-sim">Ativo</span>'
      : '<span class="badge disp-nao">Inativo</span>';
    let acoes = `<button class="btn pequeno" onclick="editarUsuario(${u.id})">Editar</button>`;
    acoes += `<button class="btn pequeno" onclick="redefinirSenhaUsuario(${u.id}, '${esc(u.nome).replace(/'/g, '')}')">Redefinir senha</button>`;
    if (!eu) {
      acoes += `<button class="btn pequeno ${u.ativo ? 'perigo' : ''}" onclick="alternarAtivoUsuario(${u.id}, ${!u.ativo}, '${esc(u.nome).replace(/'/g, '')}')">${u.ativo ? 'Desativar' : 'Reativar'}</button>`;
    }
    return `<tr${eu ? ' class="em-atendimento"' : ''}><td><b>${esc(u.nome)}</b>${eu ? ' <small>(você)</small>' : ''}</td>
      <td>${esc(u.login)}</td><td>${PERFIS[u.perfil] || u.perfil}</td><td>${situacao}</td>
      <td class="direita">${acoes}</td></tr>`;
  }).join('') || '<tr><td colspan="5" class="vazio">Nenhum usuário cadastrado.</td></tr>';
}

async function editarUsuario(id) {
  try {
    const u = await api('/usuarios/' + id);
    modoFormUsuario(u);
    $('formUsuario').scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (erro) { avisar(erro.mensagem, true); }
}

async function redefinirSenhaUsuario(id, nome) {
  const senha = prompt('Nova senha para ' + nome + ' (mínimo 6 caracteres):');
  if (senha == null) return;
  try {
    await api('/usuarios/' + id + '/senha', { method: 'POST', body: { senha } });
    avisar('Senha redefinida.');
  } catch (erro) { avisar(erro.mensagem, true); }
}

async function alternarAtivoUsuario(id, ativo, nome) {
  const acao = ativo ? 'reativar' : 'desativar';
  if (!confirm((ativo ? 'Reativar' : 'Desativar') + ' o acesso de ' + nome + '?')) return;
  try {
    await api('/usuarios/' + id + '/ativo', { method: 'PATCH', body: { ativo } });
    avisar('Usuário ' + (ativo ? 'reativado.' : 'desativado. Esse funcionário não entra mais no sistema.'));
    carregarUsuarios();
  } catch (erro) { avisar(erro.mensagem, true); }
}

$('formUsuario').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const dados = lerFormulario(e.target);
    if (!dados.medicoId) delete dados.medicoId;
    let salvo;
    if (usuarioEmEdicao) {
      delete dados.senha;
      delete dados.perfil; // perfil nao muda
      delete dados.medicoId;
      salvo = await api('/usuarios/' + usuarioEmEdicao.id, { method: 'PUT', body: dados });
      avisar('Usuário ' + salvo.nome + ' atualizado.');
    } else {
      salvo = await api('/usuarios', { method: 'POST', body: dados });
      avisar('Usuário ' + salvo.nome + ' cadastrado com sucesso.');
    }
    modoFormUsuario(null);
    carregarUsuarios();
  } catch (erro) { mostrarErros(e.target, erro); }
});
$('formUsuario').addEventListener('reset', (e) => {
  modoFormUsuario(null);
  limparErros(e.target);
});

// ======================= relatorios (rf08) =======================

/** monta o "?inicio=...&fim=..." com as datas escolhidas. */
const filtroRelatorio = () => '?' + new URLSearchParams(lerFormulario($('formRelatorio'))).toString();

async function carregarRelatorio() {
  const r = await api('/relatorios' + filtroRelatorio());
  // na primeira vez preenche as datas com o periodo padrao que a api usou (ultimos 30 dias)
  $('formRelatorio').inicio.value = r.inicio;
  $('formRelatorio').fim.value = r.fim;
  $('relatorioConteudo').innerHTML = `
    <div class="numeros">
      <div class="card">Atendimentos realizados<b>${r.totalAtendimentos}</b></div>
      <div class="card">Triagens registradas<b>${r.totalTriagens}</b></div>
      <div class="card">Pacientes atendidos<b>${r.totalPacientes}</b></div>
      <div class="card">Pacientes novos<b>${r.novosPacientes}</b></div>
      <div class="card">Cancelados<b>${r.cancelados}</b></div>
    </div><br>
    <div class="card sem-padding"><div class="barra"><h3>Por nível de gravidade (${esc(r.periodo)})</h3></div>
    <table><thead><tr><th>Gravidade</th><th>Triagens</th><th>Atendimentos</th><th>Espera média real</th><th>Tempo máximo de referência</th></tr></thead><tbody>
    ${r.porGravidade.map((l) => `<tr><td>${badge(l.gravidade)}</td><td>${l.triagens}</td><td>${l.atendimentos}</td>
      <td class="${l.tempoMedioEsperaMin > l.tempoMaximoReferenciaMin ? 'atrasado' : ''}">${l.tempoMedioEsperaMin == null ? '-' : minutosBR(l.tempoMedioEsperaMin)}</td>
      <td>${minutosBR(l.tempoMaximoReferenciaMin)}</td></tr>`).join('')}
    </tbody></table></div>`;
}

$('formRelatorio').addEventListener('submit', (e) => {
  e.preventDefault();
  carregarRelatorio().catch((erro) => avisar(erro.mensagem, true));
});

/** baixa o pdf: busca com o token e cria um link de download temporario. */
$('btnPdf').addEventListener('click', async () => {
  const r = await fetch('/api/relatorios/pdf' + filtroRelatorio(), { headers: { Authorization: 'Bearer ' + localStorage.getItem('token') } });
  if (!r.ok) return avisar('Não foi possível gerar o PDF.', true);
  const link = document.createElement('a');
  link.href = URL.createObjectURL(await r.blob());
  link.download = 'relatorio-healthcare.pdf';
  link.click();
});

// ======================= inicio =======================

/** ao abrir a pagina: se ja tem token salvo, tenta continuar logado (f5 nao derruba o usuario). */
(async function iniciar() {
  if (!localStorage.getItem('token')) return;
  try {
    const r = await api('/auth/eu');
    entrar(r.usuario, r.telaInicial);
  } catch { sair(); }
})();
