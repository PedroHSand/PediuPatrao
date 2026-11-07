// Sidebar em telas pequenas: começa fechada e só abre ao clicar no botão
const sidebar = document.getElementById('sidebar');
const botaoMenu = document.querySelector('.toggle-btn');

function fecharMenu() {
    sidebar.classList.remove('active');
}

botaoMenu.addEventListener('click', (evento) => {
    evento.stopPropagation();
    sidebar.classList.toggle('active');
});

// Fecha ao clicar fora do menu, ao escolher uma opção, com Esc e ao voltar para a página
document.addEventListener('click', (evento) => {
    if (!sidebar.contains(evento.target)) {
        fecharMenu();
    }
});
document.addEventListener('keydown', (evento) => {
    if (evento.key === 'Escape') {
        fecharMenu();
    }
});
sidebar.querySelectorAll('a[href]:not([href="#"])').forEach((link) => {
    link.addEventListener('click', fecharMenu);
});
window.addEventListener('pageshow', fecharMenu);

// Alternar submenu
function toggleSubmenu(event, element) {
    event.preventDefault();
    element.parentElement.classList.toggle('open');
}