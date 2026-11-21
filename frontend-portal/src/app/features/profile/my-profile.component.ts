import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { PlayerService } from '../../core/api/player.service';
import { Player } from '../../core/api/player.models';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NotificationService } from '../../core/ui';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './my-profile.component.html',
  styleUrl: './my-profile.component.css'
})
export class MyProfileComponent implements OnInit {
  player: Player | null = null;
  profileForm: FormGroup;
  isSaving = false;

  private readonly playerService = inject(PlayerService);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  constructor() {
    this.profileForm = this.fb.group({
      displayName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30)]]
    });
  }

  ngOnInit() {
    this.loadProfile();
  }

  loadProfile() {
    this.playerService.getMyProfile().subscribe({
      next: (data) => {
        this.player = data;
        this.profileForm.patchValue({
          displayName: data.displayName
        });
      },
      error: (err) => {
        console.error('Failed to load profile:', err);
        this.notifications.error('Failed to load profile');
      }
    });
  }

  onSubmit() {
    if (this.profileForm.invalid || this.isSaving) {
      return;
    }

    this.isSaving = true;
    const request = {
      displayName: this.profileForm.value.displayName,
    };

    this.playerService.updateMyProfile(request).subscribe({
      next: (updatedPlayer) => {
        this.player = updatedPlayer;
        this.profileForm.markAsPristine();
        this.notifications.success('Profile updated successfully');
        this.isSaving = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Failed to update profile:', err);
        this.isSaving = false;
        if (err.status === 409) {
          this.notifications.error('Display name is already taken');
        } else {
          this.notifications.error('Failed to update profile');
        }
      }
    });
  }
}
