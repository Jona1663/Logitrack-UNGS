export default function AuthLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="min-h-screen bg-gradient-to-br from-[#081c15] via-[#1b4332] to-[#081c15]">
      {children}
    </div>
  );
}
