export function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="mt-auto py-4 bg-muted/30 border-t">
      <div className="container mx-auto px-4">
        <p className="text-center text-sm text-muted-foreground">
          &copy; {currentYear} LogiTrack Agro - Sistema de Gestion Logistica
        </p>
      </div>
    </footer>
  );
}
