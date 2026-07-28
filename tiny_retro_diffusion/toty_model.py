import torch
import torch.nn as nn
import torch.nn.functional as F

class BilingualTotyEmbedding(nn.Module):
    """
    A brand-new, joint bilingual concept embedding layer created from scratch.
    It maps both Arabic and English retro and fantasy concepts into a shared
    continuous semantic space, making the model understand prompts in both languages.
    """
    def __init__(self, embed_dim=128):
        super(BilingualTotyEmbedding, self).__init__()
        self.embed_dim = embed_dim

        # Brand-new dictionary matching of essential retro concepts
        self.vocabulary = {
            "": 0, "null": 0, "none": 0,
            "mario": 1, "ماريو": 1,
            "knight": 2, "فارس": 2, "بطل": 2,
            "monster": 3, "وحش": 3, "تنين": 3,
            "coin": 4, "عملة": 4, "ذهب": 4,
            "castle": 5, "قلعة": 5, "حصن": 5,
            "sword": 6, "سيف": 6, "سلاح": 6,
            "fireball": 7, "كرة نارية": 7, "لهب": 7,
            "sky": 8, "سماء": 8, "سحاب": 8,
            "forest": 9, "غابة": 9, "شجر": 9,
            "star": 10, "نجمة": 10, "لمعان": 10
        }

        self.embedding = nn.Embedding(num_embeddings=16, embedding_dim=embed_dim)
        self.projection = nn.Sequential(
            nn.Linear(embed_dim, embed_dim),
            nn.GELU(),
            nn.Linear(embed_dim, embed_dim)
        )

    def forward(self, text_prompts):
        embeddings = []
        for prompt in text_prompts:
            p_clean = str(prompt).strip().lower()
            words = p_clean.split()
            matched_indices = []
            for w in words:
                if w in self.vocabulary:
                    matched_indices.append(self.vocabulary[w])

            if not matched_indices:
                matched_indices = [0]

            indices_tensor = torch.tensor(matched_indices, dtype=torch.long, device=self.embedding.weight.device)
            embed_vec = self.embedding(indices_tensor).mean(dim=0)
            embeddings.append(embed_vec)

        embeddings_tensor = torch.stack(embeddings, dim=0)
        return self.projection(embeddings_tensor)

class TotyResidualBlock(nn.Module):
    """
    High-performance residual block using Group Normalization and GELU activation
    optimized for lightweight CPU execution on low-RAM mobile platforms.
    """
    def __init__(self, in_channels, out_channels, is_res=True):
        super(TotyResidualBlock, self).__init__()
        self.is_res = is_res
        self.same_channels = in_channels == out_channels
        self.conv1 = nn.Sequential(
            nn.Conv2d(in_channels, out_channels, 3, 1, 1),
            nn.GroupNorm(8, out_channels),
            nn.GELU()
        )
        self.conv2 = nn.Sequential(
            nn.Conv2d(out_channels, out_channels, 3, 1, 1),
            nn.GroupNorm(8, out_channels),
            nn.GELU()
        )
        self.proj = None
        if self.is_res and not self.same_channels:
            self.proj = nn.Conv2d(in_channels, out_channels, 1, 1, 0)

    def forward(self, x):
        h = self.conv2(self.conv1(x))
        if self.is_res:
            res = self.proj(x) if self.proj else x
            return (h + res) / 1.414
        return h

class TotyUNetDown(nn.Module):
    """Contraction block (Residual Block + MaxPool)"""
    def __init__(self, in_channels, out_channels):
        super(TotyUNetDown, self).__init__()
        self.model = nn.Sequential(
            TotyResidualBlock(in_channels, out_channels, is_res=True),
            TotyResidualBlock(out_channels, out_channels, is_res=False),
            nn.MaxPool2d(2)
        )
    def forward(self, x):
        return self.model(x)

class TotyUNetUp(nn.Module):
    """Expansion block (ConvTranspose2d + residual feature concatenation)"""
    def __init__(self, in_channels, out_channels):
        super(TotyUNetUp, self).__init__()
        self.upsample = nn.ConvTranspose2d(in_channels, out_channels, 2, 2)
        self.conv = nn.Sequential(
            TotyResidualBlock(out_channels * 2, out_channels, is_res=True),
            TotyResidualBlock(out_channels, out_channels, is_res=False)
        )
    def forward(self, x, skip):
        up = self.upsample(x)
        return self.conv(torch.cat([up, skip], dim=1))

class TotyEmbedFC(nn.Module):
    """Linear embedding helper mapping scalars/latents to spatial scaling dimensions"""
    def __init__(self, input_dim, hidden_dim):
        super(TotyEmbedFC, self).__init__()
        self.model = nn.Sequential(
            nn.Linear(input_dim, hidden_dim),
            nn.GELU(),
            nn.Linear(hidden_dim, hidden_dim)
        )
    def forward(self, x):
        out = self.model(x)
        return out[:, :, None, None]

class TotyRetroDiffusionModel(nn.Module):
    """
    A brand-new, ultra-stable, and powerful context-conditioned Diffusion Model
    engineered entirely from scratch. Optimized for off-line running on any low-end
    Android phone with restricted CPU and low RAM footprint.
    """
    def __init__(self, in_channels=3, n_feat=64, embed_dim=128):
        super(TotyRetroDiffusionModel, self).__init__()
        self.in_channels = in_channels
        self.n_feat = n_feat

        # Custom bilingual embedding
        self.prompt_embedder = BilingualTotyEmbedding(embed_dim=embed_dim)

        self.init_conv = TotyResidualBlock(in_channels, n_feat, is_res=True)

        # Encoder path (64x64 -> 32x32 -> 16x16)
        self.down1 = TotyUNetDown(n_feat, n_feat * 2)
        self.down2 = TotyUNetDown(n_feat * 2, n_feat * 4)

        # Latent space Bottleneck
        self.to_vec = nn.Sequential(
            nn.AvgPool2d(16),
            nn.GELU()
        )
        self.bottleneck_up = nn.Sequential(
            nn.ConvTranspose2d(n_feat * 4, n_feat * 4, 16),
            nn.GroupNorm(8, n_feat * 4),
            nn.GELU()
        )

        # Decoder path (16x16 -> 32x32 -> 64x64)
        self.up1 = TotyUNetUp(n_feat * 4, n_feat * 2)
        self.up2 = TotyUNetUp(n_feat * 2, n_feat)

        # Final prediction layer
        self.final_conv = nn.Sequential(
            nn.Conv2d(n_feat * 2, n_feat, 3, 1, 1),
            nn.GroupNorm(8, n_feat),
            nn.GELU(),
            nn.Conv2d(n_feat, in_channels, 3, 1, 1)
        )

        # Timestep embeddings
        self.time_embed1 = TotyEmbedFC(1, n_feat * 4)
        self.time_embed2 = TotyEmbedFC(1, n_feat * 2)

        # Context prompt embeddings
        self.context_embed1 = TotyEmbedFC(embed_dim, n_feat * 4)
        self.context_embed2 = TotyEmbedFC(embed_dim, n_feat * 2)

    def forward(self, x, t, prompts):
        """
        Infers predicted noise.
        """
        c_embed = self.prompt_embedder(prompts)

        t_proj1 = self.time_embed1(t)
        t_proj2 = self.time_embed2(t)

        ctx_proj1 = self.context_embed1(c_embed)
        ctx_proj2 = self.context_embed2(c_embed)

        x_init = self.init_conv(x)

        d1 = self.down1(x_init)
        d2 = self.down2(d1)

        bottleneck = self.bottleneck_up(self.to_vec(d2))

        u1 = self.up1(bottleneck * ctx_proj1 + t_proj1, d1)
        u2 = self.up2(u1 * ctx_proj2 + t_proj2, x_init)

        out = self.final_conv(torch.cat([u2, x_init], dim=1))
        return out
